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
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {
		private ImmutableSet<Move> moves;
		private GameSetup setup;
		private Player mrX;
		private ImmutableSet<Piece> remaining;
		private ImmutableSet<Piece> winner;
		private List<Player> detectives;
		private ImmutableList<LogEntry> log;

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives)
				//final ImmutableSet<Move> moves,
				//final ImmutableSet<Piece> winner)
				{
			if (setup == null) throw new NullPointerException("setup has not gone well");
			if (mrX == null) throw new NullPointerException("mr x is dead");
			if (detectives == null) throw new NullPointerException("detectives have retired");
			if (setup.moves.isEmpty()) throw new NullPointerException("no moves!?");
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			//this.moves = moves;
			//this.winner = winner;
		}


		@Override public GameSetup getSetup() {return setup;}
		@Override public ImmutableSet<Piece> getPlayers() {return null;}
		@Override public GameState advance(Move move) {return null;}
		@Override public ImmutableSet<Move> getAvailableMoves() {return null;}
		@Override public ImmutableSet<Piece> getWinner() {return null;}
		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			return Optional.empty();}
		@Override public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) {
			return Optional.empty();}
		@Override public ImmutableList<LogEntry> getMrXTravelLog() {return null;}
	}

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		// TODO
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
		}


	}


