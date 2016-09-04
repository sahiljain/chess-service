package ca.sahiljain.chess;

import java.util.Arrays;

public class Board implements Cloneable {

    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    private static final int KING_VALUE = 20000;
    private static final int BOARD_SIZE = 8;


    Piece[][] arr;
    int oldX=-1, oldY=-1, newX=-1, newY=-1;

    Board() {
        arr = new Piece[BOARD_SIZE][BOARD_SIZE];
    }

    @Override
    public Board clone() {
        Board newBoard = new Board();
        for (int x = 0; x < BOARD_SIZE; x++) {
            newBoard.arr[x] = Arrays.copyOf(arr[x], BOARD_SIZE);
        }
        return newBoard;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Board that = (Board) obj;
            if (this.arr == null || that.arr == null) {
                return this.arr == that.arr;
            }
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (this.arr[i][j] != that.arr[i][j]) {
                        return false;
                    }
                }
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toFen() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int spaces = 0;
            for (int j = 0; j < 8; j++) {
                if (arr[i][j] == Piece.EMPTY) {
                    spaces++;
                } else {
                    if (spaces > 0) {
                        builder.append(spaces);
                        spaces = 0;
                    }
                    switch (arr[i][j]) {
                        case BLACK_BISHOP:
                            builder.append("b");
                            break;
                        case BLACK_QUEEN:
                            builder.append("q");
                            break;
                        case BLACK_KING:
                            builder.append("k");
                            break;
                        case BLACK_KNIGHT:
                            builder.append("n");
                            break;
                        case BLACK_ROOK:
                            builder.append("r");
                            break;
                        case BLACK_PAWN:
                            builder.append("p");
                            break;
                        case WHITE_BISHOP:
                            builder.append("B");
                            break;
                        case WHITE_QUEEN:
                            builder.append("Q");
                            break;
                        case WHITE_KING:
                            builder.append("K");
                            break;
                        case WHITE_KNIGHT:
                            builder.append("N");
                            break;
                        case WHITE_PAWN:
                            builder.append("P");
                            break;
                        case WHITE_ROOK:
                            builder.append("R");
                            break;
                    }
                }
            }
            if (spaces > 0) {
                builder.append(spaces);
            }
            if (i < 7) {
                builder.append("/");
            }
        }
        builder.append(" w - - 1 2");
        return builder.toString();
    }

    public Board(String fen) {
        arr = new Piece[BOARD_SIZE][BOARD_SIZE];
        String[] lines = fen.split(" ")[0].split("/");
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                arr[i][j] = Piece.EMPTY;
            }
        }
        for (int i = 0; i < 8; i++) {
            int j = 0;
            for (char c : lines[i].toCharArray()) {
                if (c >= '1' && c <= '8') {
                    j += Integer.valueOf("" + c);
                } else {
                    switch (c) {
                        case 'p':
                            arr[i][j] = Piece.BLACK_PAWN;
                            break;
                        case 'r':
                            arr[i][j] = Piece.BLACK_ROOK;
                            break;
                        case 'n':
                            arr[i][j] = Piece.BLACK_KNIGHT;
                            break;
                        case 'b':
                            arr[i][j] = Piece.BLACK_BISHOP;
                            break;
                        case 'k':
                            arr[i][j] = Piece.BLACK_KING;
                            break;
                        case 'q':
                            arr[i][j] = Piece.BLACK_QUEEN;
                            break;
                        case 'P':
                            arr[i][j] = Piece.WHITE_PAWN;
                            break;
                        case 'R':
                            arr[i][j] = Piece.WHITE_ROOK;
                            break;
                        case 'N':
                            arr[i][j] = Piece.WHITE_KNIGHT;
                            break;
                        case 'B':
                            arr[i][j] = Piece.WHITE_BISHOP;
                            break;
                        case 'K':
                            arr[i][j] = Piece.WHITE_KING;
                            break;
                        case 'Q':
                            arr[i][j] = Piece.WHITE_QUEEN;
                            break;
                    }
                    ++j;
                }
            }
        }
    }
}