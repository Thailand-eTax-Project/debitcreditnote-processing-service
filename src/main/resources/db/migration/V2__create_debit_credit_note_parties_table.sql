-- Create debit_credit_note_parties table
CREATE TABLE debit_credit_note_parties (
    id UUID PRIMARY KEY,
    note_id UUID NOT NULL REFERENCES processed_debit_credit_notes(id) ON DELETE CASCADE,
    party_type VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50) NOT NULL,
    tax_scheme VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    street_address VARCHAR(500),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_party_note_id ON debit_credit_note_parties(note_id);
CREATE INDEX idx_party_type ON debit_credit_note_parties(party_type);
