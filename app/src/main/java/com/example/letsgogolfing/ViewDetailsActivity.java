package com.example.letsgogolfing;

import static com.example.letsgogolfing.utils.Formatters.dateFormat;
import static com.example.letsgogolfing.utils.Formatters.decimalFormat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViewDetailsActivity extends AppCompatActivity {

    private String originalName;
    private String originalDescription;
    private String originalMake;
    private String originalModel;
    private String originalSerial;
    private String originalComment;
    private String originalDate;
    private String originalValue;
    private String originalTags;
    EditText name;
    EditText description;
    EditText value;
    EditText make;
    EditText model;
    EditText serial;
    EditText comment;
    EditText date;
    EditText tagsText;
    Button editButton;
    Button viewPhotoButton;
    Button saveButton;
    Button cancelButton;
    Button addPhotoButton;
    ImageButton backButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_details);

        // Retrieve the item from the intent
        Item item = (Item) getIntent().getSerializableExtra("ITEM");

        InitializeEditTextAndButtons(item);

        backButton.setOnClickListener(v -> {
            // takes back to home page main_activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        editButton.setOnClickListener(v -> {
            TransitionToEdit(v);
        });

        cancelButton.setOnClickListener(v -> {
            SetFieldsToOriginalValues(v);
            TransitionToViewItem(v);
        });

        saveButton.setOnClickListener(v -> {
            // Extract the updated information from EditText fields
            String updatedName = name.getText().toString();
            String updatedDescription = description.getText().toString();
            String updatedMake = make.getText().toString();
            String updatedModel = model.getText().toString();
            String updatedSerialNumber = serial.getText().toString();
            String updatedComment = comment.getText().toString();
            String updatedDate = date.getText().toString();
            String updatedValueString = value.getText().toString();

            Date updatedDateOfPurchase = null;
            try {
                // Convert the date string back to a Date object
                updatedDateOfPurchase = dateFormat.parse(updatedDate);
            } catch (ParseException e) {
                e.printStackTrace();
                Toast.makeText(ViewDetailsActivity.this, "Invalid date format", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert the value string to a double
            double updatedEstimatedValue;
            try {
                updatedEstimatedValue = Double.parseDouble(updatedValueString);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Toast.makeText(ViewDetailsActivity.this, "No Value Entered. Defaulted to 0.", Toast.LENGTH_SHORT).show();
                updatedEstimatedValue = 0;

                return;
            }

            // Create a Map for the updated values
            Map<String, Object> updatedValues = new HashMap<>();
            updatedValues.put("name", updatedName);
            updatedValues.put("description", updatedDescription);
            updatedValues.put("make", updatedMake);
            updatedValues.put("model", updatedModel);
            updatedValues.put("serialNumber", updatedSerialNumber);
            updatedValues.put("comment", updatedComment);
            updatedValues.put("dateOfPurchase", updatedDateOfPurchase != null ? new Timestamp(updatedDateOfPurchase) : null);
            updatedValues.put("estimatedValue", updatedEstimatedValue);

            // Get the document ID from the item
            String documentId = item.getId(); // Assuming 'item' is an instance variable representing the current item

            // Update Firestore document
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("items").document(documentId)
                    .update(updatedValues)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ViewDetailsActivity.this, "Changes saved", Toast.LENGTH_SHORT).show();
                        TransitionToViewItem(v);
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(ViewDetailsActivity.this, "Error updating item", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void TransitionToEdit(View v) {
        saveButton.setVisibility(v.VISIBLE);
        cancelButton.setVisibility(v.VISIBLE);
        addPhotoButton.setVisibility(v.VISIBLE);
        viewPhotoButton.setVisibility(v.INVISIBLE);
        editButton.setVisibility(v.INVISIBLE);
        name.setEnabled(true);
        description.setEnabled(true);
        model.setEnabled(true);
        make.setEnabled(true);
        serial.setEnabled(true);
        comment.setEnabled(true);
        date.setEnabled(true);
        value.setEnabled(true);
        tagsText.setEnabled(true);
    }
    private void TransitionToViewItem(View v) {
        saveButton.setVisibility(v.INVISIBLE);
        cancelButton.setVisibility(v.INVISIBLE);
        addPhotoButton.setVisibility(v.INVISIBLE);
        viewPhotoButton.setVisibility(v.VISIBLE);
        editButton.setVisibility(v.VISIBLE);
        name.setEnabled(false);
        description.setEnabled(false);
        model.setEnabled(false);
        make.setEnabled(false);
        serial.setEnabled(false);
        comment.setEnabled(false);
        date.setEnabled(false);
        value.setEnabled(false);
        tagsText.setEnabled(false);
    }

    private void SetFieldsToOriginalValues(View v) {
        name.setText(originalName);
        description.setText(originalDescription);
        make.setText(originalMake);
        model.setText(originalModel);
        serial.setText(originalSerial);
        comment.setText(originalComment);
        date.setText(originalDate);
        value.setText(originalValue);
        tagsText.setText(originalTags);
    }

    private void InitializeEditTextAndButtons(Item item) {

        // Initialize EditTexts
        name = findViewById(R.id.nameField);
        description = findViewById(R.id.descriptionField);
        make = findViewById(R.id.makeField);
        model = findViewById(R.id.modelField);
        serial = findViewById(R.id.serialField);
        comment = findViewById(R.id.commentField);
        date = findViewById(R.id.dateField);
        value = findViewById(R.id.valueField);
        tagsText = findViewById(R.id.tagsField);
        // Initialize Buttons
        saveButton = findViewById(R.id.saveBtn);
        editButton = findViewById(R.id.editInfoBtn);
        cancelButton = findViewById(R.id.cancel_edit_button);
        addPhotoButton = findViewById(R.id.add_photo_button);
        backButton = findViewById(R.id.backButton);
        viewPhotoButton = findViewById(R.id.viewPhotoBtn);

        // Set original values for when cancel is pressed
        originalName = item.getName();
        originalDescription = item.getDescription();
        originalMake = item.getMake();
        originalModel = item.getModel();
        originalSerial = item.getSerialNumber();
        originalComment = item.getComment();
        originalDate = dateFormat.format(item.getDateOfPurchase());
        originalValue = decimalFormat.format(item.getEstimatedValue());
        originalTags = TextUtils.join(", ", item.getTags());

        // Set the EditTexts with the original values
        name.setText(originalName);
        description.setText(originalDescription);
        make.setText(originalMake);
        model.setText(originalModel);
        serial.setText(originalSerial);
        comment.setText(originalComment);
        date.setText(originalDate);
        value.setText(originalValue);
        tagsText.setText(originalTags);

        // Set fields to not be editable at first
        name.setEnabled(false);
        description.setEnabled(false);
        model.setEnabled(false);
        make.setEnabled(false);
        serial.setEnabled(false);
        comment.setEnabled(false);
        date.setEnabled(false);
        value.setEnabled(false);
        tagsText.setEnabled(false);
    }
}
