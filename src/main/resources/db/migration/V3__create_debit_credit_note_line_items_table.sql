-- Create debit_credit_note_line_items table
CREATE TABLE debit_credit_note_line_items (
    id UUID PRIMARY KEY,
    note_id UUID NOT NULL REFERENCES processed_debit_credit_notes(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    description VARCHAR(1000) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    tax_rate DECIMAL(5,2) NOT NULL,
    line_total DECIMAL(15,2) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    total_with_tax DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_line_note_id ON debit_credit_note_line_items(note_id);

ALTER TABLE debit_credit_note_line_items
ADD CONSTRAINT uq_note_line_number UNIQUE (note_id, line_number);
