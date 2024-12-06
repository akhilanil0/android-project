package com.example.sudokuapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import java.util.Random;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.widget.TextView;
import android.app.AlertDialog;


public class MainActivity extends AppCompatActivity {

    private static final int GRID_SIZE = 9;
    private EditText[][] cells = new EditText[GRID_SIZE][GRID_SIZE];
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private int prefilledCells = 40; // Default to Easy difficulty
    private Random random = new Random();
    private EditText selectedCell = null;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private Button btnClear;
    private Button btnHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnClear = findViewById(R.id.btn_clear);
        btnHint = findViewById(R.id.btn_hint);
        Button btnReset = findViewById(R.id.btn_reset);
        btnClear.setEnabled(false); // Disable initially

        GridLayout sudokuGrid = findViewById(R.id.sudoku_grid);
        Spinner difficultySpinner = findViewById(R.id.difficulty_spinner);
        Button btnGenerate = findViewById(R.id.btn_generate);
        Button btnSolve = findViewById(R.id.btn_solve);

        initializeGrid(sudokuGrid);
        initializeNumberPad();

        difficultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // Easy
                        prefilledCells = 40;
                        break;
                    case 1: // Medium
                        prefilledCells = 30;
                        break;
                    case 2: // Hard
                        prefilledCells = 20;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefilledCells = 40;
            }
        });

        btnGenerate.setOnClickListener(view -> {
            clearGrid();
            generatePuzzle();
        });

        btnSolve.setOnClickListener(view -> {
            if (isValidPuzzle()) {
                if (solveSudoku()) {
                    Toast.makeText(this, "Solved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No valid solution exists!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Invalid puzzle configuration!", Toast.LENGTH_SHORT).show();
            }
        });

        btnClear.setOnClickListener(view -> {
            if (selectedCell != null) {
                new AlertDialog.Builder(this)
                    .setTitle("Confirm Clear")
                    .setMessage("Are you sure you want to clear the selected cell?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        selectedCell.setText("");
                        selectedCell.setEnabled(true);
                        selectedCell.setBackgroundResource(R.drawable.cell_background);
                        selectedCell = null; // Reset selected cell
                        btnClear.setEnabled(false); // Disable clear button again
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            }
        });

        btnReset.setOnClickListener(view -> {
            clearGrid(); // Clear the grid
            btnClear.setEnabled(false); // Disable clear button
        });

        btnHint.setOnClickListener(view -> {
            if (selectedCell != null && selectedCell.isEnabled()) {
                int row = selectedRow;
                int col = selectedCol;
                int hintValue = getHintValue(row, col);
                if (hintValue != -1) {
                    selectedCell.setText(String.valueOf(hintValue));
                    validateCell(selectedCell, row, col);
                } else {
                    Toast.makeText(this, "No hint available for this cell!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initializeGrid(GridLayout sudokuGrid) {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                EditText cell = new EditText(this);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(20);
                cell.setTypeface(Typeface.DEFAULT_BOLD);
                cell.setTextColor(getResources().getColor(R.color.cell_filled));
                cell.setBackgroundResource(R.drawable.cell_background);
                cell.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                cell.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
                cell.setPadding(0, 0, 0, 0);

                final int finalRow = row;
                final int finalCol = col;

                cell.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (s.length() > 0) {
                            try {
                                int num = Integer.parseInt(s.toString());
                                if (num < 1 || num > 9) {
                                    cell.setText("");
                                } else {
                                    validateCell(cell, finalRow, finalCol);
                                }
                            } catch (NumberFormatException e) {
                                cell.setText("");
                            }
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = getResources().getDimensionPixelSize(R.dimen.cell_size);
                params.height = getResources().getDimensionPixelSize(R.dimen.cell_size);
                params.setMargins(1, 1, 1, 1);
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                cell.setLayoutParams(params);
                sudokuGrid.addView(cell);
                cells[row][col] = cell;

                cell.setFocusable(false);
                cell.setClickable(true);

                cell.setOnClickListener(v -> {
                    if (selectedCell != null) {
                        selectedCell.setBackgroundResource(R.drawable.cell_background);
                    }
                    selectedCell = cell;
                    selectedRow = finalRow;
                    selectedCol = finalCol;
                    if (cell.isEnabled()) {
                        cell.setBackgroundResource(R.drawable.cell_background_selected);
                        btnClear.setEnabled(true);
                        btnHint.setEnabled(true);
                    } else {
                        btnHint.setEnabled(false);
                    }
                });
            }
        }
    }

    private void initializeNumberPad() {
        LinearLayout numberPad = findViewById(R.id.number_pad);
        int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9};

        for (int number : numbers) {
            TextView numberText = new TextView(this);
            numberText.setText(String.valueOf(number));
            numberText.setTextSize(28);
            numberText.setTextColor(getResources().getColor(R.color.number_pad_text));
            numberText.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                getResources().getDimensionPixelSize(R.dimen.number_pad_size), 1);
            params.setMargins(4, 8, 4, 8);
            numberText.setLayoutParams(params);

            numberText.setOnClickListener(v -> {
                if (selectedCell != null && selectedCell.isEnabled()) {
                    selectedCell.setText(String.valueOf(number));
                    validateCell(selectedCell, selectedRow, selectedCol);
                }
            });

            numberPad.addView(numberText);
        }
    }

    private void generatePuzzle() {
        int[][] board = new int[GRID_SIZE][GRID_SIZE];
        fillDiagonal();
        if (solveSudoku()) {
            int numbersToRemove = (GRID_SIZE * GRID_SIZE) - prefilledCells;
            while (numbersToRemove > 0) {
                int row = random.nextInt(GRID_SIZE);
                int col = random.nextInt(GRID_SIZE);
                if (!cells[row][col].getText().toString().isEmpty()) {
                    cells[row][col].setText("");
                    cells[row][col].setEnabled(true);
                    numbersToRemove--;
                }
            }
        }

        // Disable the pre-filled cells
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (!cells[row][col].getText().toString().isEmpty()) {
                    cells[row][col].setEnabled(false); // Disable editing for pre-filled cells
                }
            }
        }
    }

    private void fillDiagonal() {
        for (int i = 0; i < GRID_SIZE; i += 3) {
            fillBox(i, i);
        }
    }

    private void fillBox(int row, int col) {
        int num;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                do {
                    num = random.nextInt(9) + 1;
                } while (!isValidInBox(row, col, num));
                cells[row + i][col + j].setText(String.valueOf(num));
            }
        }
    }

    private boolean isValidInBox(int rowStart, int colStart, int num) {
        String numStr = String.valueOf(num);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (cells[rowStart + i][colStart + j].getText().toString().equals(numStr)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void clearGrid() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                cells[i][j].setText("");
                cells[i][j].setEnabled(true);
            }
        }
    }

    private boolean solveSudoku() {
        int row = -1;
        int col = -1;
        boolean isEmpty = true;

        for (int i = 0; i < GRID_SIZE && isEmpty; i++) {
            for (int j = 0; j < GRID_SIZE && isEmpty; j++) {
                if (cells[i][j].getText().toString().isEmpty()) {
                    row = i;
                    col = j;
                    isEmpty = false;
                }
            }
        }

        if (isEmpty) {
            return true;
        }

        for (int num = 1; num <= GRID_SIZE; num++) {
            if (isValid(row, col, num)) {
                cells[row][col].setText(String.valueOf(num));
                if (solveSudoku()) {
                    return true;
                }
                cells[row][col].setText("");
            }
        }
        return false;
    }

    private boolean isValid(int row, int col, int num) {
        String numStr = String.valueOf(num);

        for (int x = 0; x < GRID_SIZE; x++) {
            if (cells[row][x].getText().toString().equals(numStr)) {
                return false;
            }
        }

        for (int x = 0; x < GRID_SIZE; x++) {
            if (cells[x][col].getText().toString().equals(numStr)) {
                return false;
            }
        }

        int startRow = row - row % 3;
        int startCol = col - col % 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (cells[i + startRow][j + startCol].getText().toString().equals(numStr)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void validateCell(EditText cell, int row, int col) {
        String value = cell.getText().toString();

        if (!value.isEmpty()) {
            int num = Integer.parseInt(value);
            if (num < 1 || num > 9) {
                cell.setText("");
                cell.setBackgroundResource(R.drawable.cell_background_error); // Highlight error
                return;
            }

            if (!isValid(row, col, num)) {
                cell.setBackgroundResource(R.drawable.cell_background_error); // Highlight error
                cell.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            } else {
                cell.setTextColor(getResources().getColor(R.color.cell_filled));
                cell.setBackgroundResource(R.drawable.cell_background);
            }

            // Check if the Sudoku is solved after filling the cell
            if (isSudokuSolved()) {
                Toast.makeText(this, "Sudoku Solved!", Toast.LENGTH_SHORT).show();
            }
        } else {
            cell.setBackgroundResource(R.drawable.cell_background);
        }
    }

    private boolean isValidPuzzle() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                String value = cells[i][j].getText().toString();
                if (!value.isEmpty()) {
                    int num = Integer.parseInt(value);
                    cells[i][j].setText("");
                    if (!isValid(i, j, num)) {
                        cells[i][j].setText(String.valueOf(num));
                        return false;
                    }
                    cells[i][j].setText(String.valueOf(num));
                }
            }
        }
        return true;
    }

    private int getHintValue(int row, int col) {
        if (cells[row][col].getText().toString().isEmpty()) {
            for (int num = 1; num <= GRID_SIZE; num++) {
                if (isValid(row, col, num)) {
                    return num;
                }
            }
        }
        return -1;
    }

    private boolean isSudokuSolved() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                String value = cells[row][col].getText().toString();
                if (value.isEmpty() || !isValid(row, col, Integer.parseInt(value))) {
                    return false; // If any cell is empty or invalid, return false
                }
            }
        }
        return true; // All cells are filled and valid
    }
}
