package com.example.letsgogolfing;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.letsgogolfing.utils.ItemAdapter;
import com.google.firebase.appcheck.BuildConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.example.letsgogolfing.CameraActivity.MODE_PHOTO_CAMERA;
import static com.example.letsgogolfing.utils.Formatters.decimalFormat;
import java.util.Set;


/**
 * The main activity class that serves as the entry point for the application.
 * It handles the display and interaction with a grid of items, allowing the user to
 * select and delete items, as well as adding new ones and viewing their details.
 */
public class MainActivity extends AppCompatActivity implements SortDialogFragment.SortOptionListener {
    private TextView selectTextCancel; // Add this member variable for the TextView
    private static final String TAG = "MainActivity";
    private GridView itemGrid;
    private ItemAdapter itemAdapter; // You need to create this Adapter class.

    private boolean isSelectMode = false;
    private ImageButton selectButton;
    private ImageButton deleteButton;
    private ItemComparator comparator;
    private ImageView scanItemButton;

    private FirestoreRepository firestoreRepository;
    private DialogFragment sortDialog = new SortDialogFragment();

    @Override
    public void onSortOptionSelected(String selectedOption, boolean sortDirection) {
        comparator = new ItemComparator(selectedOption, sortDirection);
        sortArrayAdapter(comparator);
    }

    private void sortArrayAdapter(Comparator<Item> comparator) {
        if (itemAdapter != null) {
            ArrayList<Item> itemList = new ArrayList<>();
            for (int i = 0; i < itemAdapter.getCount(); i++) {
                itemList.add(itemAdapter.getItem(i));
            }

            itemList.sort(comparator);
            itemAdapter.clear();
            itemAdapter.addAll(itemList);
            itemAdapter.notifyDataSetChanged();
        }
    }


    /**
     * Initializes the activity with the required layout and sets up the item grid adapter.
     * It also configures click listeners for the item grid and other UI components.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the most recent data,
     *                           or null if it is the first time.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isDebugMode() || isRunningEspressoTest()) {
            // Bypass login and directly initialize components
            // Initialize with test or default data
            initForTesting();
        } else {
            SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            String currentUsername = sharedPref.getString("username", null);
            if (currentUsername == null || currentUsername.isEmpty()) {
                // Redirect to LoginActivity
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "Crashed in known location", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }


            // Initialize FirestoreRepository with the current username
            firestoreRepository = new FirestoreRepository(currentUsername);

        }

        itemGrid = findViewById(R.id.itemGrid);
        itemAdapter = new ItemAdapter(this, new ArrayList<>());
        itemGrid.setAdapter(itemAdapter);

        fetchItemsAndRefreshAdapter();

        itemGrid.setOnItemLongClickListener((parent, view, position, id) -> {
            Item item = itemAdapter.getItem(position);
            if (item != null && item.getId() != null) {
                // Proceed with deletion
                List<String> itemIdsToDelete = Collections.singletonList(item.getId());
                firestoreRepository.deleteItems(itemIdsToDelete, new FirestoreRepository.OnItemDeletedListener() {
                    @Override
                    public void OnItemsDeleted() {
                        itemAdapter.removeItem(position);
                        itemAdapter.notifyDataSetChanged();
                        updateTotalValue(itemAdapter.getItems());
                        Toast.makeText(MainActivity.this, "Item deleted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(MainActivity.this, "Error deleting item", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Document ID is null, handle this case
                Toast.makeText(MainActivity.this, "Cannot delete item without an ID", Toast.LENGTH_SHORT).show();
            }

            return true; // True to indicate the long click was consumed
        });

        itemGrid.setOnItemClickListener((parent, view, position, id) -> {
            if (isSelectMode) {
                itemAdapter.toggleSelection(position); // Toggle item selection
            } else {
                Item item = itemAdapter.getItem(position);
                if (item != null && item.getId() != null) {
                    firestoreRepository.fetchItemById(item.getId(), new FirestoreRepository.OnItemFetchedListener() {
                        @Override
                        public void onItemFetched(Item fetchedItem) {
                            Intent intent = new Intent(MainActivity.this, ViewDetailsActivity.class);
                            intent.putExtra("item", fetchedItem); // Pass the fetched item
                            startActivity(intent);
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(MainActivity.this, "Error fetching item from database", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        ImageView addItemButton = findViewById(R.id.addItemButton);
        addItemButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddItemActivity.class);
            startActivity(intent);
        });

        Button manageTagsButton = findViewById(R.id.manage_tags_button);
        manageTagsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ManageTagsActivity.class);
            startActivity(intent);
        });
        ImageButton sortButton = findViewById(R.id.sort_button);
        sortButton.setOnClickListener(v -> {
            sortDialog.show(getSupportFragmentManager(), "SortDialogFragment");

        });
        // Clicking the profile button
        ImageView profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ViewProfileActivity.class);
            startActivity(intent);
        });

        scanItemButton = findViewById(R.id.scan_item_button);
        scanItemButton.setOnClickListener(v -> {
            // Check for camera permission before launching the camera
                Intent photoIntent = new Intent(this, CameraActivity.class);
                photoIntent.putExtra("mode", MODE_PHOTO_CAMERA);
                photoIntent.putExtra("BarcodeInfo", true);
                startActivity(photoIntent);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchItemsAndRefreshAdapter();
    }


    /**
     * Updates the total value text view with the sum of estimated values of all items.
     *
     * @param items The list of items whose values are to be summed.
     */
    private void updateTotalValue(List<Item> items) {
        double totalValue = 0;
        for (Item item : items) {
            totalValue += item.getEstimatedValue(); // Assuming getEstimatedValue() returns a double
        }

        TextView totalValueTextView = findViewById(R.id.totalValue);
        totalValueTextView.setText(this.getApplicationContext().getString(R.string.item_value , decimalFormat.format(totalValue)));
    }

    private boolean isRunningEspressoTest() {
        // Check for a system property that you will set in your test setup
        return "true".equals(System.getProperty("isRunningEspressoTest"));
    }

    private void initForTesting() {
        // Initialize components as needed for testing
        // This might include setting up dummy data or mocks
        // For example:
        firestoreRepository = new FirestoreRepository("testUser");
        itemGrid = findViewById(R.id.itemGrid);
        itemAdapter = new ItemAdapter(this, new ArrayList<>()); // Use a test adapter if necessary
        itemGrid.setAdapter(itemAdapter);
        // Other initializations...
    }



    private boolean isDebugMode() {
        return BuildConfig.DEBUG;
    }


    /**
     * Fetches items from the Firestore database and updates the grid adapter.
     * It also updates the total value of all items displayed.
     */
    private void fetchItemsAndRefreshAdapter() {
        // I changed this so that we use the FirestoreRepo class to handle the database - (vedant)
        firestoreRepository.fetchItems(new FirestoreRepository.OnItemsFetchedListener() {
            @Override
            public void onItemsFetched(List<Item> items) {
                itemAdapter.updateItems(items);
                sortArrayAdapter(comparator);
                updateTotalValue(items);
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Error getting documents: ", e);
            }
        });
    }

    /**
     * Deletes the selected items from the Firestore database and updates the UI accordingly.
     * It clears the selection mode after deletion is completed.
     */
    private void deleteSelectedItems() {
        Set<Integer> selectedPositions = itemAdapter.getSelectedPositions();
        List<String> itemIdsToDelete = new ArrayList<>();
        for (int position : selectedPositions) {
            Item item = itemAdapter.getItem(position);
            itemIdsToDelete.add(item.getId());
        }

        firestoreRepository.deleteItems(itemIdsToDelete, new FirestoreRepository.OnItemDeletedListener() {
            @Override
            public void OnItemsDeleted() {
                // Remove items from the adapter and refresh
                List<Integer> positions = new ArrayList<>(selectedPositions);
                Collections.sort(positions, Collections.reverseOrder());
                for (int position : positions) {
                    itemAdapter.removeItem(position);
                }
                itemAdapter.clearSelection();
                itemAdapter.notifyDataSetChanged();
                updateTotalValue(itemAdapter.getItems());
                Toast.makeText(MainActivity.this, "Items deleted", Toast.LENGTH_SHORT).show();

                // Reset select mode
                isSelectMode = false;
                itemAdapter.setSelectModeEnabled(false);
                deleteButton.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Error deleting items", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
