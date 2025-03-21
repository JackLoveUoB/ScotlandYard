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

		private MyGameState( //constructor
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
			//check validity of each detective
			Set<Integer> detectiveLocations = new HashSet<>();
			for (Player detective : detectives) { //check whether the detectives are cheating and copying eachother
				if (!detective.piece().isDetective()) throw new IllegalArgumentException("detective is a liar and not real");
				if (!detectiveLocations.add(detective.location())) throw new IllegalArgumentException("detectiveLocations is duplicate");
				//cheaty double ticket
				if (detective.has(Ticket.DOUBLE) || detective.has(Ticket.SECRET)) throw new IllegalArgumentException("detective has double ticket");
			}

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			Set<Move> moveSet = new HashSet<>(); //set of legal moves

			//if mrx turn then give him his moves
			if (remaining.isEmpty()) {
				moveSet.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
				moveSet.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
			}

			// add moves for each detective in remaining
			for (Player detective : detectives) {
				if (remaining.contains(detective.piece())) moveSet.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
			}

			this.moves = ImmutableSet.copyOf(moveSet);

			boolean noDetectiveTickets = detectives.stream() // check if detectives are all out of tickets
					.allMatch(detective -> detective.tickets().entrySet().stream()
							.allMatch(entry -> entry.getValue() == 0));

			// check if mrX has completed all moves in setup.moves
			boolean mrXFinishedMoves = (remaining.isEmpty() && log.size() >= setup.moves.size());

			// check if mrX has been captured by a detective
			boolean mrXcaptured = detectiveLocations.contains(mrX.location());

			// check if mrX has won, checking if mrX has no available moves or all detectives have no tickets, or mrX has finished all his moves
			if (!remaining.isEmpty() && this.moves.isEmpty() || noDetectiveTickets || mrXFinishedMoves) {
				this.winner = ImmutableSet.of(mrX.piece());
				this.moves = ImmutableSet.of();
			}
			// check if detectives have won, checking if the detectives have captured mrX, or the detectives have no avaiable moves
			else if (mrXcaptured || (remaining.isEmpty() && this.moves.isEmpty())) {
				this.winner = ImmutableSet.copyOf(getPlayers().stream().filter(x -> x != mrX.piece()).collect(Collectors.toSet()));
				this.moves = ImmutableSet.of();
			}
			// no one has won
			else this.winner = winner;
		}

		@Override public GameSetup getSetup() { return setup; }

		@Override public ImmutableSet<Piece> getPlayers() { //gives all players in game
			Set<Piece> playersSet = new HashSet<>();
			playersSet.add(mrX.piece());
			for (Player detective : detectives) {
				playersSet.add(detective.piece());
			}

			return ImmutableSet.copyOf(playersSet);
		}

		//apply a move and create new game state from move
		//use visitor differ singles and doubles
		@Override public GameState advance(Move move) {
			if (!moves.contains(move)) throw new IllegalArgumentException("move is breaking the law and illegal");

			//visiting time
			return move.accept(new Move.Visitor<GameState>() {
				@Override public GameState visit(SingleMove singleMove) {
					if (move.commencedBy().equals(mrX.piece())) {
						List<LogEntry> newLog = new ArrayList<>(log);
						//if mrx made the move, add it to the log and decide whether to reveal or hide the move
						if (setup.moves.get(log.size())) newLog.add(LogEntry.reveal(singleMove.ticket, singleMove.destination));
						else newLog.add(LogEntry.hidden(singleMove.ticket));

						Player mrXUseTicket = mrX.use(singleMove.ticket);
						Player mrXNewLocation = mrXUseTicket.at(singleMove.destination);

						//create new set of remaining that adds the detectives
						Set<Piece> newRemaining = new HashSet<>();
						for (Player detective : detectives) {
							// only add the detective to newRemaining if it has tickets
							if (detective.tickets().entrySet().stream().anyMatch(entry -> entry.getValue() != 0)) newRemaining.add(detective.piece());
						}

						return new MyGameState(setup, ImmutableSet.copyOf(newRemaining), ImmutableList.copyOf(newLog), mrXNewLocation, detectives, moves, winner);
					}

					Player detective = detectives.stream()
							.filter(d -> d.piece().equals(move.commencedBy()))
							.findFirst()
							.orElse(null); //since the move is by a detective, filter to find detective that made the move
					Player detectiveNewLocation = detective.at(singleMove.destination);
					Player detectiveUseTicket = detectiveNewLocation.use(singleMove.ticket);
					//mrx steals the ticket
					Player mrXGiveTicket = mrX.give(singleMove.ticket);
					List<Player> newDetectives = new ArrayList<>(detectives);
					newDetectives.set(detectives.indexOf(detective), detectiveUseTicket);

					//remove the detective that just moved
					Set<Piece> newRemaining = new HashSet<>(remaining);
					newRemaining.remove(move.commencedBy());

					return new MyGameState(setup, ImmutableSet.copyOf(newRemaining), log, mrXGiveTicket, newDetectives, moves, winner);
				}

				@Override public GameState visit(DoubleMove doubleMove) {
					//adds two entries to the log for both moves
					List<LogEntry> newLog = new ArrayList<>(log);
					if (setup.moves.get(log.size())) newLog.add(LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1));
					else newLog.add(LogEntry.hidden(doubleMove.ticket1));
					if (setup.moves.get(log.size() + 1)) newLog.add(LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2));
					else newLog.add(LogEntry.hidden(doubleMove.ticket2));

					//take away his double ticket
					Player mrXUseTickets = mrX.use(List.of(doubleMove.ticket1, doubleMove.ticket2, Ticket.DOUBLE));
					Player mrXNewLocation = mrXUseTickets.at(doubleMove.destination2);

					//new remaining moves made, same as visit(SingleMove singleMove)
					Set<Piece> newRemaining = new HashSet<>();
					for (Player detective : detectives) {
						if (detective.tickets().entrySet().stream().anyMatch(entry -> entry.getValue() != 0)) newRemaining.add(detective.piece());
					}

					return new MyGameState(setup, ImmutableSet.copyOf(newRemaining), ImmutableList.copyOf(newLog), mrXNewLocation, detectives, moves, winner);
				}
			});
		}

		@Override public ImmutableSet<Move> getAvailableMoves() { return moves; }

		@Override public ImmutableSet<Piece> getWinner() { return winner; }

		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player detective_ : detectives) {
				if (detective_.piece() == detective) return Optional.of(detective_.location());
			}

			return Optional.empty();
		}

		//should show the ticket count for a piece
		@Override public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) {
			for (Player detective : detectives) {
				// check if piece is detective
				if (detective.piece().equals(piece)) {
					return Optional.of(new TicketBoard() {
						@Override
						public int getCount(@Nonnull Ticket ticket) {
							return detective.tickets().get(ticket);
						}
					});
				}

				// check if piece is mrX
				if (mrX.piece().equals(piece)) {
					return Optional.of(new Board.TicketBoard() {
						@Override
						public int getCount(@Nonnull Ticket ticket) {
							return mrX.tickets().get(ticket);
						}
					});
				}
			}

			return Optional.empty();
		}
		@Override public ImmutableList<LogEntry> getMrXTravelLog() { return log; }
	}

	@Nonnull @Override public GameState build(
			GameSetup setup, //CONTAINS THE GRAPH <<<------------
			//^^^^^^^^^^^^^^ GRAPH HERE
			Player mrX,
			ImmutableList<Player> detectives) {
		ImmutableList<LogEntry> log = ImmutableList.of();
		ImmutableSet<Move> moves = ImmutableSet.of();
		ImmutableSet<Piece> winner = ImmutableSet.of();
		ImmutableSet<Piece> remaining = ImmutableSet.of();
		// needs to have 7 inputs since 7 values are returned into a test from mygamestate
		return new MyGameState(setup, remaining, log, mrX, detectives, moves, winner);
	}

	//generate all valid moves from a node in the graph
	private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		Set<SingleMove> singleMoves = new HashSet<>();

		// check each adjacent node
		for (int destination : setup.graph.adjacentNodes(source)) {
			boolean empty = true;

			for (Player detective : detectives) { //stop thieving of peoples spaces
                if (detective.location() == destination) {
                    empty = false;
                    break; // we should find a better way of implementing this without break later
                }
			}

			if (empty) {
				//if the person has a ticket for the available transport then the move is also added
				for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if (player.has(t.requiredTicket())) {
						singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
					}
				}

				//the elusive *secret* ticket
				if (player.has(Ticket.SECRET)) {
					singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
				}
			}
		}

		return singleMoves;
	}

	//make all of the available moves mrx can double jump to
	private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player mrX, int source) {
		Set<DoubleMove> doubleMoves = new HashSet<>();

		if (!mrX.has(Ticket.DOUBLE) || !(mrX.has(Ticket.TAXI) || mrX.has(Ticket.BUS) || mrX.has(Ticket.UNDERGROUND) || mrX.has(Ticket.SECRET)) || setup.moves.size() <= 1) return doubleMoves;

		//start at the source and look at single moves
		for (int destination1 : setup.graph.adjacentNodes(source)) {
			boolean empty1 = true;

			for (Player detective : detectives) {
				if (detective.location() == destination1) {
					empty1 = false;
					break;
				}
			}

			if (empty1) {
				//look at second single moves from destination1
				for (int destination2 : setup.graph.adjacentNodes(destination1)) {
					boolean empty2 = true;

					for (Player detective : detectives) {
						if (detective.location() == destination2) {
							empty2 = false;
							break;
						}
					}

					if (empty2) {
						//for each transport option of destination1
						for (Transport t1 : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of())) {
							if (mrX.has(t1.requiredTicket())) {
								//for each transport option of destination2
								for (Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) {
									//bug occured where he can move twice with 1 car ticket for example
									//make sure he has to have two
									if (t1.requiredTicket().equals(t2.requiredTicket()) && !mrX.hasAtLeast(t1.requiredTicket(), 2)) continue;

									if (mrX.has(t2.requiredTicket())) { //ONLY if he has the required ticket
										doubleMoves.add(new DoubleMove(mrX.piece(), source, t1.requiredTicket(), destination1, t2.requiredTicket(), destination2));
									}
								}
							}

							// the *secret* special ticket for the second move
							if (mrX.has(Ticket.SECRET)) {
								doubleMoves.add(new DoubleMove(mrX.piece(), source, t1.requiredTicket(), destination1, Ticket.SECRET, destination2));
							}
						}

						//what if instead, secret time was first?
						if (mrX.has(Ticket.SECRET)) {
							for (Transport t : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) {
								if (mrX.has(t.requiredTicket())) {
									doubleMoves.add(new DoubleMove(mrX.piece(), source, Ticket.SECRET, destination1, t.requiredTicket(), destination2));
								}
							}

							//not sure how two secret tickets
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


