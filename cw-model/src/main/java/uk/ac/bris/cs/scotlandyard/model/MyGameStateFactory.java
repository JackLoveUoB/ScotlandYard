package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

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
				if (detective.has(Ticket.DOUBLE) || detective.has(Ticket.SECRET)) throw new IllegalArgumentException("detective has double ticket");
			}

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = winner;

			Set<Move> moveSet = new HashSet<>();

//			for (Player detective : detectives) {
//				moveSet.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
//			}

			moveSet.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
			moveSet.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));

			this.moves = ImmutableSet.copyOf(moveSet);
		}


		@Override public GameSetup getSetup() {return setup;}
		@Override public ImmutableSet<Piece> getPlayers() {
			Set<Piece> playersSet = new HashSet<>();
			playersSet.add(mrX.piece());
			for (Player detective : detectives) {
				playersSet.add(detective.piece());
			}

			return ImmutableSet.copyOf(playersSet);
		}
		@Override public GameState advance(Move move) {
			if (!moves.contains(move)) throw new IllegalArgumentException("move is breaking the law and illegal");
			return move.accept(new Move.Visitor<GameState>() {
				@Override public GameState visit(SingleMove singleMove) {
					if (mrX.piece().isMrX()) {
						Map<Ticket, Integer> newMrXTickets = new HashMap<>(mrX.tickets());
						Ticket oldTicket = singleMove.ticket;
						int numby = newMrXTickets.getOrDefault(oldTicket, 0);
						if (numby <= 0) throw new IllegalStateException("no ticket found");
						newMrXTickets.put(oldTicket, numby - 1);
						Player mrXIsBack = new Player(mrX.piece(), (ImmutableMap<Ticket, Integer>) newMrXTickets, singleMove.destination);
						ImmutableSet<Piece> newRemaining = detectives.stream()
								.map(Player::piece)
								.collect(ImmutableSet.toImmutableSet());
						//ImmutableSet<Move> newMoves =
					}
					return null;
				}
				@Override public GameState visit(DoubleMove doubleMove) {
					return null;
				}
			});
		}
		@Override public ImmutableSet<Move> getAvailableMoves() { return moves; }
		@Override public ImmutableSet<Piece> getWinner() {return winner;}
		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player detectivey : detectives) {
				if (detectivey.piece().equals(detective)) return Optional.of(detectivey.location());
			} //go through all detectives in list to see if detective exists. if does not exits then empty
			return Optional.empty();} //this was auto completed by the ide no idea if it works
		@Override public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) {
			for (Player detective : detectives) {
				if (detective.piece() == piece) {
					return Optional.of(new TicketBoard() {
						@Override
						public int getCount(@Nonnull Ticket ticket) {
							return detective.tickets().get(ticket);
						}
					});
				}

				if (mrX.piece() == piece) {
					return Optional.of(new Board.TicketBoard() {
						@Override
						public int getCount(@Nonnull Ticket ticket) {
							return mrX.tickets().get(ticket);
						}
					});
				}

				return Optional.empty();
			}

			return Optional.empty();
		}
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

	private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		Set<SingleMove> singleMoves = new HashSet<>();

		for (int destination : setup.graph.adjacentNodes(source)) {
			boolean empty = true;

			for (Player detective : detectives) {
                if (detective.location() == destination) {
                    empty = false;
                    break;
                }
			}

			if (empty) {
				for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if (player.has(t.requiredTicket())) {
						singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
					}
				}

				if (player.has(Ticket.SECRET)) {
					singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
				}
			}
		}

		return singleMoves;
	}

	private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player mrX, int source) {
		Set<DoubleMove> doubleMoves = new HashSet<>();

		if (!mrX.has(Ticket.DOUBLE) || !(mrX.has(Ticket.TAXI) || mrX.has(Ticket.BUS) || mrX.has(Ticket.UNDERGROUND) || mrX.has(Ticket.SECRET)) || setup.moves.size() <= 1) return doubleMoves;

		for (int destination1 : setup.graph.adjacentNodes(source)) {
			boolean empty1 = true;

			for (Player detective : detectives) {
				if (detective.location() == destination1) {
					empty1 = false;
					break;
				}
			}

			if (empty1) {
				for (int destination2 : setup.graph.adjacentNodes(destination1)) {
					boolean empty2 = true;

					for (Player detective : detectives) {
						if (detective.location() == destination2) {
							empty2 = false;
							break;
						}
					}

					if (empty2) {
						for (Transport t1 : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of())) {
							if (mrX.has(t1.requiredTicket())) {
								for (Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) {
									if (t1.requiredTicket().equals(t2.requiredTicket()) && !mrX.hasAtLeast(t1.requiredTicket(), 2))
										continue;

									if (mrX.has(t2.requiredTicket())) {
										doubleMoves.add(new DoubleMove(mrX.piece(), source, t1.requiredTicket(), destination1, t2.requiredTicket(), destination2));
									}
								}
							}

							if (mrX.has(Ticket.SECRET)) {
								doubleMoves.add(new DoubleMove(mrX.piece(), source, t1.requiredTicket(), destination1, Ticket.SECRET, destination2));
							}
						}

						if (mrX.has(Ticket.SECRET)) {
							for (Transport t : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) {
								if (mrX.has(t.requiredTicket())) {
									doubleMoves.add(new DoubleMove(mrX.piece(), source, Ticket.SECRET, destination1, t.requiredTicket(), destination2));
								}
							}

							if (mrX.hasAtLeast(Ticket.SECRET, 2)) {
								doubleMoves.add(new DoubleMove(mrX.piece(), source, Ticket.SECRET, destination1, Ticket.SECRET, destination2));
							}
						}
					}
				}
			}
		}

		return doubleMoves;
	}
}


