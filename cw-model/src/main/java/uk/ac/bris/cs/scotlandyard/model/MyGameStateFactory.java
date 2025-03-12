package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * cw-model
 * Stage 1: Complete this class
 */

// builds new game state off of factory
public final class MyGameStateFactory implements Factory<GameState> {
	//implements game state
	private final class MyGameState implements GameState {
		private ImmutableSet<Move> moves; //moves that can be done
		private GameSetup setup;
		private Player mrX;
		private ImmutableSet<Piece> remaining; //pieces that have yet to move
		private ImmutableSet<Piece> winner; //	if finished then who wins
		private List<Player> detectives;
		private ImmutableList<LogEntry> log; //where the mr man is running to

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives,
				final ImmutableSet<Move> moves,
				final ImmutableSet<Piece> winner)
		{
			//check if we failed on setup and made things empty
			if (setup == null) throw new NullPointerException("setup has not gone well");
			if (mrX == null) throw new NullPointerException("mr x is dead");
			if (detectives == null) throw new NullPointerException("detectives have retired");
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("no moves!?");
			if (!mrX.piece().isMrX()) throw new IllegalArgumentException("only MrX can move twice thanks");
			Set<Integer> detectiveLocations = new HashSet<>();
			for (Player detective : detectives) { //check whether the detectives are cheating and copying eachother
				if (!detective.piece().isDetective()) throw new IllegalArgumentException("detective is a liar and not real");
				if (!detectiveLocations.add(detective.location())) throw new IllegalArgumentException("detectiveLocations is duplicate");
			}

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.moves = moves;
			this.winner = winner;
		}


		@Override public GameSetup getSetup() {return setup;}
		@Override public ImmutableSet<Piece> getPlayers() {return null;}
		@Override public GameState advance(Move move) {
			if (!moves.contains(move)) throw new IllegalArgumentException("move is breaking the law and illegal");
			return null;
		}
		@Override public ImmutableSet<Move> getAvailableMoves() {return moves;}
		@Override public ImmutableSet<Piece> getWinner() {return winner;}
		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			if (!detectives.contains(detective)) return Optional.empty();
			return Optional.of(detectives.indexOf(detective));} //this was auto completed by the ide no idea if it works
		@Override public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) {
			return Optional.empty();}
		@Override public ImmutableList<LogEntry> getMrXTravelLog() {return log;}
	}

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		// TODO
		ImmutableList<LogEntry> log = ImmutableList.of();
		ImmutableSet<Move> moves = ImmutableSet.of();
		ImmutableSet<Piece> winner = ImmutableSet.of();
		ImmutableSet<Piece> remaining = ImmutableSet.of();
		// needs to have 7 inputs since 7 values are returned into a test from mygamestate
		// no idea why this order tho
		return new MyGameState(setup, remaining, log, mrX, detectives, moves, winner);
		}


	}


