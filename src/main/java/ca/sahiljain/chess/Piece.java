package ca.sahiljain.chess;

enum Piece {
    WHITE_ROOK, WHITE_BISHOP, WHITE_KING, WHITE_QUEEN, WHITE_KNIGHT, WHITE_PAWN, BLACK_ROOK, BLACK_BISHOP, BLACK_KING, BLACK_QUEEN, BLACK_KNIGHT, BLACK_PAWN, EMPTY;

    public boolean isBlack() {
        return this == BLACK_KING || this == BLACK_ROOK || this == BLACK_BISHOP || this == BLACK_QUEEN || this == BLACK_KNIGHT || this == BLACK_PAWN;
    }

    public boolean isWhite() {
        return this == WHITE_KING || this == WHITE_ROOK || this == WHITE_BISHOP || this == WHITE_QUEEN || this == WHITE_KNIGHT || this == WHITE_PAWN;
    }

    public boolean canBeReplacedBy(Piece replacer) {
        return this == EMPTY || this.isWhite() && replacer.isBlack() || this.isBlack() && replacer.isWhite();
    }
}
