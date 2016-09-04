package ca.sahiljain.chess;

import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.httpservice.HttpService;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Middleware;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.SyncHandler;

import java.util.Optional;

public final class App {

    public static void main(String[] args) throws LoadingException {
        HttpService.boot(App::init, "chess-service", args);
    }

    static void init(Environment environment) {
        SyncHandler<Response<String>> moveHandler = context -> playMove(context.request());

        environment.routingEngine()
                .registerAutoRoute(Route.with(exceptionHandler(), "GET", "/move", moveHandler));
    }

    static Response<String> playMove(Request request) {
        Optional<String> fen = request.parameter("fen");
        if (fen.isPresent()) {
            Board board = new Board(fen.get());
            Board newBoard = ChessStatic.playComputerMove(Player.MINIMIZER, board);
            String newFen = newBoard.toFen();
            return Response.forPayload(newFen).withHeader("Access-Control-Allow-Origin", "*");
        } else {
            return Response.forStatus(Status.BAD_REQUEST);
        }
    }

    /**
     * A generic middleware that maps uncaught exceptions to error code 418
     */
    static <T> Middleware<SyncHandler<Response<T>>, SyncHandler<Response<T>>> exceptionMiddleware() {
        return handler -> requestContext -> {
            try {
                return handler.invoke(requestContext);
            } catch (RuntimeException e) {
                return Response.forStatus(Status.IM_A_TEAPOT);
            }
        };
    }

    /**
     * Async version of {@link #exceptionMiddleware()}
     */
    static <T> Middleware<SyncHandler<Response<T>>, AsyncHandler<Response<T>>> exceptionHandler() {
        return App.<T>exceptionMiddleware().and(Middleware::syncToAsync);
    }
}