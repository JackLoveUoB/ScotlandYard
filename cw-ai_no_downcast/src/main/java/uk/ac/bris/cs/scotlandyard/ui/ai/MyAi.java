package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "alex and jack's ai"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		Piece mrXPiece = board.getPlayers().stream()
				.filter(Piece::isMrX)
				.toList().get(0);

		ImmutableMap.Builder<Ticket, Integer> mrXTickets = ImmutableMap.builder();

		for (Ticket ticket : ScotlandYard.MRX_TICKETS) {
			mrXTickets.put(ticket, Objects.requireNonNull(board.getPlayerTickets(mrXPiece).orElse(null)).getCount(ticket));
		}

		int mrXLocation = board.getAvailableMoves().stream().toList().get(0).source();

		Player mrX = new Player(MRX, mrXTickets.build(), mrXLocation);

		List<Piece> detectivePieces = board.getPlayers().stream()
				.filter(Predicate.not(Piece::isMrX))
				.toList();

		List<Player> detectives = new ArrayList<>();

		for (Piece piece : detectivePieces) {
			ImmutableMap.Builder<Ticket, Integer> detectiveTickets = ImmutableMap.builder();

			for (Ticket ticket : ScotlandYard.DETECTIVE_TICKETS) {
				detectiveTickets.put(ticket, Objects.requireNonNull(board.getPlayerTickets(piece).orElse(null)).getCount(ticket));
			}

			Detective detective = (Detective) piece;

			int detectiveLocation = board.getDetectiveLocation(detective).orElse(0);

			detectives.add(new Player(piece, detectiveTickets.build(), detectiveLocation));
		}

		System.out.println(mrXScore(board, mrX, detectives));

		var moves = board.getAvailableMoves().asList();
		return moves.get(new Random().nextInt(moves.size()));
	}

	public int mrXScore(@Nonnull Board board, Player mrX, List<Player> detectives) {
		int score = 0;

		for (int destination : board.getSetup().graph.adjacentNodes(mrX.location())) {
			for (Player detective : detectives) {
				if (detective.location() == destination) score++;
			}
		}

		return score;
	}
}
