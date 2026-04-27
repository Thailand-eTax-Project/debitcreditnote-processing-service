-- Create processed_debit_credit_notes table
CREATE TABLE processed_debit_credit_notes (
    id UUID PRIMARY KEY,
    source_note_id VARCHAR(100) NOT NULL,
    note_number VARCHAR(50) NOT NULL,
    note_type VARCHAR(20) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL,
    total_tax DECIMAL(15,2) NOT NULL,
    total DECIMAL(15,2) NOT NULL,
    original_xml TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_note_number ON processed_debit_credit_notes(note_number);
CREATE INDEX idx_source_note_id ON processed_debit_credit_notes(source_note_id);
CREATE INDEX idx_status ON processed_debit_credit_notes(status);
CREATE INDEX idx_issue_date ON processed_debit_credit_notes(issue_date);

CREATE UNIQUE INDEX idx_note_number_unique ON processed_debit_credit_notes(note_number);
