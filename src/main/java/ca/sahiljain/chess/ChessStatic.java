package ca.sahiljain.chess;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ChessStatic {

    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    private static final int KING_VALUE = 20000;
    private static final int BOARD_SIZE = 8;

    static class Node {
        Board board;
        int value;
        int alpha;
        int beta;
        ArrayList<Node> children;

        Node(Board board) {
            this.board = board;
            this.value = 0;
            this.children = null;
            alpha = Integer.MIN_VALUE;
            beta = Integer.MAX_VALUE;
        }
    }

    private static void evaluateNodes(Node root, Player player, int depth, boolean pruning) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        if (gameOver(root.board) || depth == 0) {
            root.value = evaluateBoard(root.board, player.opposite());
        } else {
            root.value = player == Player.MAXIMIZER ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            root.children = new ArrayList<>();

            for (Board childBoard : getChildren(root.board, player)) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                Node newChild = new Node(childBoard);
                newChild.alpha = root.alpha;
                newChild.beta = root.beta;
                evaluateNodes(newChild, player.opposite(), depth-1, pruning);
                root.children.add(newChild);
                if (player == Player.MAXIMIZER) {
                    root.value = Math.max(root.value, newChild.value);
                    root.alpha = Math.max(root.alpha, newChild.value);
                } else {
                    root.value = Math.min(root.value, newChild.value);
                    root.beta = Math.min(root.beta, newChild.value);
                }
                if (pruning && root.beta <= root.alpha) {
                    break;
                }
            }
        }
    }

    public static boolean gameOver(Board board) {
        return doesHeWin(board, Player.MAXIMIZER) || doesHeWin(board, Player.MINIMIZER);
    }

    private static Node minChild(ArrayList<Node> children) {
        Node min = children.get(0);
        for (int i = 1; i < children.size(); i++) {
            if (children.get(i).value < min.value) {
                min = children.get(i);
            }
        }
        return min;
    }

    private static Node maxChild(ArrayList<Node> children) {
        Node max = children.get(0);
        for (int i = 1; i < children.size(); i++) {
            if (children.get(i).value > max.value) {
                max = children.get(i);
            }
        }
        return max;
    }

    public static Board playComputerMove(final Player player, final Board currentBoard) {
        long startTime = System.currentTimeMillis();
        Node root = new Node(currentBoard);
        try {
            evaluateNodes(root, player, 1, true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Node child = player == Player.MINIMIZER ? minChild(root.children) : maxChild(root.children);
        final int[] depth = {1};

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Node> task = new Callable<Node>() {
            @Override
            public Node call() throws Exception {
                Node innerRoot = new Node(currentBoard);
                evaluateNodes(innerRoot, player, ++depth[0], true);
                return player == Player.MINIMIZER ? minChild(innerRoot.children) : maxChild(innerRoot.children);
            }
        };

        while (true) {
            long timeRemaining = startTime + 7 * 1000 - System.currentTimeMillis();
            if (timeRemaining <= 0 || depth[0] > 35) {
                break;
            }
            Future<Node> future = executor.submit(task);
            try {
                Node node = future.get(timeRemaining, TimeUnit.MILLISECONDS);
                child = node;
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.out.println("cancelled");
            }
        }

        executor.shutdownNow();
        System.out.println("depth: " + depth[0]);
        return child.board;
    }

    public static int evaluateBoard(Board board, Player player) {
        if (gameOver(board)) {
            if (doesHeWin(board, Player.MAXIMIZER)) {
                return Integer.MAX_VALUE;
            }
            if (doesHeWin(board, Player.MINIMIZER)) {
                return Integer.MIN_VALUE;
            }
            return 0;
        }
        return evalMaterial(board) + 10*evalDomination(board) + evalMobility(board);
    }

    private static int evalMobility(Board board) {
        return getChildren(board, Player.MAXIMIZER).size() - getChildren(board, Player.MINIMIZER).size();
    }


    private static int evalDomination(Board board) {
        int whiteValue = 0;
        int blackValue = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                Piece piece = board.arr[i][j];
                if (piece == Piece.WHITE_PAWN) {
                    whiteValue += BOARD_SIZE - i - 1;
                } else if (piece == Piece.BLACK_PAWN) {
                    blackValue += i;
                }
            }
        }
        return whiteValue - blackValue;
    }

    private static int evalMaterial(Board board) {
        //white material - black material
        int whiteValue = 0;
        int blackValue = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                Piece piece = board.arr[i][j];
                if (piece == Piece.WHITE_KING) {
                    whiteValue += KING_VALUE;
                } else if (piece == Piece.BLACK_KING) {
                    blackValue += KING_VALUE;
                } else if (piece == Piece.WHITE_ROOK) {
                    whiteValue += ROOK_VALUE;
                } else if (piece == Piece.BLACK_ROOK) {
                    blackValue += ROOK_VALUE;
                } else if (piece == Piece.WHITE_BISHOP) {
                    whiteValue += BISHOP_VALUE;
                } else if (piece == Piece.BLACK_BISHOP) {
                    blackValue += BISHOP_VALUE;
                } else if (piece == Piece.BLACK_QUEEN) {
                    blackValue += QUEEN_VALUE;
                } else if (piece == Piece.WHITE_QUEEN) {
                    whiteValue += QUEEN_VALUE;
                } else if (piece == Piece.BLACK_KNIGHT) {
                    blackValue += KNIGHT_VALUE;
                } else if (piece == Piece.WHITE_KNIGHT) {
                    whiteValue += KNIGHT_VALUE;
                } else if (piece == Piece.BLACK_PAWN) {
                    blackValue += PAWN_VALUE;
                } else if (piece == Piece.WHITE_PAWN) {
                    whiteValue += PAWN_VALUE;
                }
            }
        }
        return whiteValue - blackValue;
    }

    public static boolean doesHeWin(Board board, Player player) {
        boolean hasBlackKing = false;
        boolean hasWhiteKing = false;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board.arr[i][j] == Piece.WHITE_KING) {
                    hasWhiteKing = true;
                } else if (board.arr[i][j] == Piece.BLACK_KING) {
                    hasBlackKing = true;
                }
            }
        }

        if (player == Player.MAXIMIZER) {
            return !hasBlackKing;
        } else {
            return !hasWhiteKing;
        }
    }

    public static List<Board> getChildren(Board board, Player player) {
        List<Board> children = new ArrayList<>();

        Piece kingPiece;
        Piece rookPiece;
        Piece bishopPiece;
        Piece queenPiece;
        Piece knightPiece;
        Piece pawnPiece;
        if (player == Player.MAXIMIZER) {
            kingPiece = Piece.WHITE_KING;
            rookPiece = Piece.WHITE_ROOK;
            bishopPiece = Piece.WHITE_BISHOP;
            queenPiece = Piece.WHITE_QUEEN;
            knightPiece = Piece.WHITE_KNIGHT;
            pawnPiece = Piece.WHITE_PAWN;
        } else {
            kingPiece = Piece.BLACK_KING;
            rookPiece = Piece.BLACK_ROOK;
            bishopPiece = Piece.BLACK_BISHOP;
            queenPiece = Piece.BLACK_QUEEN;
            knightPiece = Piece.BLACK_KNIGHT;
            pawnPiece = Piece.BLACK_PAWN;
        }
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board.arr[i][j] == kingPiece) {
                    moveKingAround(kingPiece, i, j, board, children);
                } else if (board.arr[i][j] == rookPiece) {
                    moveRookAround(rookPiece, i, j, board, children);
                } else if (board.arr[i][j] == bishopPiece) {
                    moveBishopAround(bishopPiece, i, j, board, children);
                } else if (board.arr[i][j] == queenPiece) {
                    moveQueenAround(queenPiece, i, j, board, children);
                } else if (board.arr[i][j] == knightPiece) {
                    moveKnightAround(knightPiece, i, j, board, children);
                } else if (board.arr[i][j] == pawnPiece) {
                    movePawnAround(pawnPiece, i, j, board, children);
                }
            }
        }
        return children;
    }

    private static void movePawnAround(Piece pawnPiece, int i, int j, Board board, List<Board> children) {
        if (pawnPiece.isBlack() && i == 1 && board.arr[2][j] == Piece.EMPTY && board.arr[3][j] == Piece.EMPTY) {
            Board newBoard = board.clone();
            newBoard.arr[1][j] = Piece.EMPTY;
            newBoard.arr[3][j] = pawnPiece;
            newBoard.oldX = 1;
            newBoard.oldY = j;
            newBoard.newX = 3;
            newBoard.newY = j;
            children.add(newBoard);
        }
        if (pawnPiece.isWhite() && i == 6 && board.arr[5][j] == Piece.EMPTY && board.arr[4][j] == Piece.EMPTY) {
            Board newBoard = board.clone();
            newBoard.arr[6][j] = Piece.EMPTY;
            newBoard.arr[4][j] = pawnPiece;
            newBoard.oldX = 6;
            newBoard.oldY = j;
            newBoard.newX = 4;
            newBoard.newY = j;
            children.add(newBoard);
        }
        if (pawnPiece.isBlack() && i < 7 && board.arr[i+1][j] == Piece.EMPTY) {
            Board newBoard = board.clone();
            newBoard.arr[i][j] = Piece.EMPTY;
            if (i+1 == BOARD_SIZE-1) {
                newBoard.arr[i+1][j] = Piece.BLACK_QUEEN;
            } else {
                newBoard.arr[i+1][j] = pawnPiece;
            }
            newBoard.oldX = i;
            newBoard.oldY = j;
            newBoard.newX = i+1;
            newBoard.newY = j;
            children.add(newBoard);
        }
        if (pawnPiece.isWhite() && i > 0 && board.arr[i-1][j] == Piece.EMPTY) {
            Board newBoard = board.clone();
            newBoard.arr[i][j] = Piece.EMPTY;
            if (i-1 == 0) {
                newBoard.arr[i-1][j] = Piece.WHITE_QUEEN;
            } else {
                newBoard.arr[i-1][j] = pawnPiece;
            }
            newBoard.oldX = i;
            newBoard.oldY = j;
            newBoard.newX = i-1;
            newBoard.newY = j;
            children.add(newBoard);
        }
        if (pawnPiece.isBlack() && i < 7 && j > 0 && board.arr[i+1][j-1].isWhite()) {
            Board newBoard = board.clone();
            newBoard.arr[i][j] = Piece.EMPTY;
            if (i+1 == BOARD_SIZE-1) {
                newBoard.arr[i+1][j-1] = Piece.BLACK_QUEEN;
            } else {
                newBoard.arr[i+1][j-1] = pawnPiece;
            }
            newBoard.oldX = i;
            newBoard.oldY = j;
            newBoard.newX = i+1;
            newBoard.newY = j-1;
            children.add(newBoard);
        }
        if (pawnPiece.isBlack() && i < 7 && j < 7 && board.arr[i+1][j+1].isWhite()) {
            Board newBoard = board.clone();
            newBoard.arr[i][j] = Piece.EMPTY;
            if (i+1 == BOARD_SIZE-1) {
                newBoard.arr[i+1][j+1] = Piece.BLACK_QUEEN;
            } else {
                newBoard.arr[i+1][j+1] = pawnPiece;
            }
            newBoard.oldX = i;
            newBoard.oldY = j;
            newBoard.newX = i+1;
            newBoard.newY = j+1;
            children.add(newBoard);
        }
        if (pawnPiece.isWhite() && i > 0 && j > 0 && board.arr[i-1][j-1].isBlack()) {
            Board newBoard = board.clone();
            newBoard.arr[i][j] = Piece.EMPTY;
            if (i-1 == 0) {
                newBoard.arr[i-1][j-1] = Piece.WHITE_QUEEN;
            } else {
                newBoard.arr[i-1][j-1] = pawnPiece;
            }
            newBoard.oldX = i;
            newBoard.oldY = j;
            newBoard.newX = i-1;
            newBoard.newY = j-1;
            children.add(newBoard);
        }
        if (pawnPiece.isWhite() && i > 0 && j < 7 && board.arr[i-1][j+1].isBlack()) {
            Board newBoard = board.clone();
            newBoard.arr[i][j] = Piece.EMPTY;
            if (i-1 == 0) {
                newBoard.arr[i-1][j+1] = Piece.WHITE_QUEEN;
            } else {
                newBoard.arr[i-1][j+1] = pawnPiece;
            }
            newBoard.oldX = i;
            newBoard.oldY = j;
            newBoard.newX = i-1;
            newBoard.newY = j+1;
            children.add(newBoard);
        }
    }

    private static void moveQueenAround(Piece queenPiece, int i, int j, Board board, List<Board> children) {
        moveRookAround(queenPiece, i, j, board, children);
        moveBishopAround(queenPiece, i, j, board, children);
    }

    private static void moveBishopAround(Piece bishopPiece, int i, int j, Board board, List<Board> children) {
        for (int newI = i, newJ = j; newI>0 && newJ>0;) {
            if (board.arr[newI-1][newJ-1].canBeReplacedBy(bishopPiece)) {
                Board newBoard = board.clone();
                newBoard.arr[i][j] = Piece.EMPTY;
                newBoard.arr[newI-1][newJ-1] = bishopPiece;
                newBoard.oldX = i;
                newBoard.oldY = j;
                newBoard.newX = newI-1;
                newBoard.newY = newJ-1;
                children.add(newBoard);
            }
            if (board.arr[newI-1][newJ-1] != Piece.EMPTY) {
                //attack here, and then stop trying
                break;
            }
            newI--;newJ--;
        }
        for (int newI = i, newJ = j; newI<BOARD_SIZE-1 && newJ<BOARD_SIZE-1;) {
            if (board.arr[newI+1][newJ+1].canBeReplacedBy(bishopPiece)) {
                Board newBoard = board.clone();
                newBoard.arr[i][j] = Piece.EMPTY;
                newBoard.arr[newI+1][newJ+1] = bishopPiece;
                newBoard.oldX = i;
                newBoard.oldY = j;
                newBoard.newX = newI+1;
                newBoard.newY = newJ+1;
                children.add(newBoard);
            }
            if (board.arr[newI+1][newJ+1] != Piece.EMPTY) {
                //attack here, and then stop trying
                break;
            }
            newI++;newJ++;
        }
        for (int newI = i, newJ = j; newI>0 && newJ<BOARD_SIZE-1;) {
            if (board.arr[newI-1][newJ+1].canBeReplacedBy(bishopPiece)) {
                Board newBoard = board.clone();
                newBoard.arr[i][j] = Piece.EMPTY;
                newBoard.arr[newI-1][newJ+1] = bishopPiece;
                newBoard.oldX = i;
                newBoard.oldY = j;
                newBoard.newX = newI-1;
                newBoard.newY = newJ+1;
                children.add(newBoard);
            }
            if (board.arr[newI-1][newJ+1] != Piece.EMPTY) {
                //attack here, and then stop trying
                break;
            }
            newI--;newJ++;
        }
        for (int newI = i, newJ = j; newI<BOARD_SIZE-1 && newJ>0;) {
            if (board.arr[newI+1][newJ-1].canBeReplacedBy(bishopPiece)) {
                Board newBoard = board.clone();
                newBoard.arr[i][j] = Piece.EMPTY;
                newBoard.arr[newI+1][newJ-1] = bishopPiece;
                newBoard.oldX = i;
                newBoard.oldY = j;
                newBoard.newX = newI+1;
                newBoard.newY = newJ-1;
                children.add(newBoard);
            }
            if (board.arr[newI+1][newJ-1] != Piece.EMPTY) {
                //attack here, and then stop trying
                break;
            }
            newI++;newJ--;
        }
    }

    private static void moveRookAround(Piece rookPiece, int i, int j, Board board, List<Board> children) {
        //playMove it left (i--)
        for (int newI = i; newI>0; newI--) {
            if (board.arr[newI-1][j].canBeReplacedBy(rookPiece)) {
                Board newBoard = board.clone();
                newBoard.arr[i][j] = Piece.EMPTY;
                newBoard.arr[newI-1][j] = rookPiece;
                newBoard.oldX = i;
                newBoard.oldY = j;
                newBoard.newX = newI-1;
                newBoard.newY = j;
                children.add(newBoard);
            }
            if (board.arr[newI-1][j] != Piece.EMPTY) {
                //attack here, and then stop trying
                break;
            }
        }
        for (int newI = i; newI<BOARD_SIZE-1; newI++) {
            if (board.arr[newI+1][j].canBeReplacedBy(rookPiece)) {
                Board newBoard = board.clone();
                newBoard.arr[i][j] = Piece.EMPTY;
                newBoard.arr[newI+1][j] = rookPiece;
                newBoard.oldX = i;
                newBoard.oldY = j;
                newBoard.newX = newI+1;
                newBoard.newY = j;
                children.add(newBoard);
            }
            if (board.arr[newI+1][j] != Piece.EMPTY) {
                //attack here, and then stop trying
                break;
            }
        }
        for (int newJ = j; newJ>0; newJ--) {
            if (board.arr[i][newJ-1].canBeReplacedBy(rookPiece)) {
                Board newBoard = board.clone();
                newBoard.arr[i][j] = Piece.EMPTY;
                newBoard.arr[i][newJ-1] = rookPiece;
                newBoard.oldX = i;
                newBoard.oldY = j;
                newBoard.newX = i;
                newBoard.newY = newJ-1;
                children.add(newBoard);
            }
            if (board.arr[i][newJ-1] != Piece.EMPTY) {
                //attack here, and then stop trying
                break;
            }
        }
        for (int newJ = j; newJ<BOARD_SIZE-1; newJ++) {
            if (board.arr[i][newJ+1].canBeReplacedBy(rookPiece)) {
                Board newBoard = board.clone();
                newBoard.arr[i][j] = Piece.EMPTY;
                newBoard.arr[i][newJ+1] = rookPiece;
                newBoard.oldX = i;
                newBoard.oldY = j;
                newBoard.newX = i;
                newBoard.newY = newJ+1;
                children.add(newBoard);
            }
            if (board.arr[i][newJ+1] != Piece.EMPTY) {
                //attack here, and then stop trying
                break;
            }
        }
    }

    private static void moveKingAround(Piece kingPiece, int i, int j, Board board, List<Board> children) {
        movePieceAround(kingPiece, i, j, i +1, j, board, children);
        movePieceAround(kingPiece, i, j, i -1, j, board, children);
        movePieceAround(kingPiece, i, j, i, j +1, board, children);
        movePieceAround(kingPiece, i, j, i, j -1, board, children);
        movePieceAround(kingPiece, i, j, i -1, j -1, board, children);
        movePieceAround(kingPiece, i, j, i -1, j +1, board, children);
        movePieceAround(kingPiece, i, j, i +1, j -1, board, children);
        movePieceAround(kingPiece, i, j, i +1, j +1, board, children);
    }

    private static void moveKnightAround(Piece knightPiece, int i, int j, Board board, List<Board> children) {
        movePieceAround(knightPiece, i, j, i+2, j+1, board, children);
        movePieceAround(knightPiece, i, j, i+2, j-1, board, children);
        movePieceAround(knightPiece, i, j, i-2, j+1, board, children);
        movePieceAround(knightPiece, i, j, i-2, j-1, board, children);
        movePieceAround(knightPiece, i, j, i-1, j+2, board, children);
        movePieceAround(knightPiece, i, j, i+1, j+2, board, children);
        movePieceAround(knightPiece, i, j, i-1, j-2, board, children);
        movePieceAround(knightPiece, i, j, i+1, j-2, board, children);
    }

    private static void movePieceAround(Piece piece, int oldX, int oldY, int newX, int newY, Board board, List<Board> children) {
        if (newX <= BOARD_SIZE-1 && newX >= 0 && newY <= BOARD_SIZE-1 && newY >= 0) {
            if (board.arr[newX][newY] == Piece.EMPTY || board.arr[newX][newY].canBeReplacedBy(piece)) {
                Board newBoard = board.clone();
                newBoard.arr[oldX][oldY] = Piece.EMPTY;
                newBoard.arr[newX][newY] = piece;
                newBoard.oldX = oldX;
                newBoard.oldY = oldY;
                newBoard.newX = newX;
                newBoard.newY = newY;
                children.add(newBoard);
            }
        }
    }
}
