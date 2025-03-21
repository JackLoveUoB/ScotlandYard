package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

//wrap within model
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		Board.GameState initialState = new MyGameStateFactory().build(setup, mrX, detectives);
		//this is the wrapping
		return new MyModel(initialState);
	}

	private static final class MyModel implements Model {
		private Board.GameState state; //the last game state returned
		private final Set<Observer> observers; //observers that are spying on the game
		//try notify of changes as accurately as can for now
		private MyModel(Board.GameState initialState) {
			if (initialState == null) throw new NullPointerException("helmp");
			this.state = initialState;
			this.observers = new HashSet<>();
		}

		@Nonnull @Override public Board getCurrentBoard() {
			return state;
		}

		@Override public void registerObserver (@Nonnull Observer observer) {
			//add new observer to get updates
			if (observer == null) throw new NullPointerException("helmp");
			if (!observers.add(observer)) throw new IllegalArgumentException("duplicater");
		}

		@Override public void unregisterObserver (@Nonnull Observer observer) {
			//banish observer person from the game
			if (observer == null) throw new NullPointerException("helmp");
			if (!observers.remove(observer)) throw new IllegalArgumentException("tough crowd");
		}

		@Nonnull @Override public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
		}

		//check whether a move was made or the game ended
		//if a move was made then apply it and reflect it to observers
		@Override public void chooseMove (@Nonnull Move move) {
			if (move == null) throw new NullPointerException("helmp");
			state = state.advance(move);
			final Observer.Event event;
			ImmutableSet<Piece> winners = state.getWinner();
			if (!winners.isEmpty()) {
				event = Observer.Event.GAME_OVER;
			} else {
				event = Observer.Event.MOVE_MADE;
			}
			//notify time
			for (Observer observer : observers) {
				observer.onModelChanged(state, event);
			}
		}

	}


}
