package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Lib;

public class Boat {
	static BoatGrader bg;
	static int childrenOnOahu = 0;
	static int childrenOnMolokai = 0;
	static int adultsOnOahu = 0;
	static int adultsOnMolokai = 0;
	static int total = 0;
	static int passengers = 0;
	static boolean complete = false;
	static Lock boat;
	static Condition2 waitingOnOahu;
	static Condition2 waitingOnMolokai;
	static Condition2 riding;
	static int boatLocation = 0;
	static final int Oahu = 0;
	static final int Molokai = 1;
	static Communicator done;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		// System.out.println("\n ***Testing Boats with only 2 children***"); // should
		// pass
		// begin(0, 2, b);
		//
		// System.out.println("\n ***Testing Boats with 2 children, 1 adult***"); //
		// should pass
		// begin(1, 2, b);
		//
		// System.out.println("\n ***Testing Boats with 3 children, 3 adults***"); //
		// should pass
		// begin(3, 3, b);
		//
		// System.out.println("\n Testing with 1 child and 1 adult"); // should fail
		// begin(1,1,b);
		//
		// System.out.println("\n Testing with 2 child and 100 adult"); // should pass
		// begin(100,2,b);
		//
		// System.out.println("\n Testing with 100 child and 1 adult"); // should pass
		// begin(1, 100, b);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here
		boat = new Lock();
		waitingOnOahu = new Condition2(boat);
		waitingOnMolokai = new Condition2(boat);
		riding = new Condition2(boat);
		done = new Communicator();
		complete = false;
		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.

		// Create runnable that houses local variable of location for adult threads
		// and will increase counter for starting island of adults on Oahu
		Runnable c = new Runnable() {
			public void run() {
				int location = Oahu;
				ChildItinerary(location);
			}
		};
		/*
		 * Runnable r = new Runnable() { public void run() { SampleItinerary(); } };
		 * KThread t = new KThread(r); t.setName("Sample Boat Thread"); t.fork();
		 */
		// Create children threads and fork to new thread
		for (int i = 0; i < children; i++) {
			KThread t = new KThread(c);
			t.setName("Child " + (i + 1));
			// System.out.println(t.getName() + " created");
			childrenOnOahu++;
			t.fork();
		}
		// Create runnable that houses local variable of location for children threads
		// and will increase counter for starting island of children on Oahu
		Runnable a = new Runnable() {
			public void run() {
				int location = Oahu;
				AdultItinerary(location);
			}
		};
		// Create adults threads and fork to new thread
		for (int i = 0; i < adults; i++) {
			KThread t = new KThread(a);
			t.setName("Adult " + (i + 1));
			// System.out.println(t.getName() + " created");
			adultsOnOahu++;
			t.fork();
		}
		// Final check to see if all threads are on Molokai
		while (!complete) {
			if (done.listen() == adults + children)
				complete = true;
		}
		System.out.println("All children and adults on Molokai");
	}

	static void AdultItinerary(int location) {
		/*
		 * This is where you should put your solutions. Make calls to the BoatGrader to
		 * show that it is synchronized. For example: bg.AdultRowToMolokai(); indicates
		 * that an adult has rowed the boat across to Molokai
		 */
		while (!complete) {
			boat.acquire();
			// System.out.println(KThread.currentThread().getName() + " called");
			// Below used to debug to make sure threads are called correctly
			// System.out.println(KThread.currentThread().getName() + " called");
			if (location == Oahu) {
				// If any passengers are on the boat, if the number of children on Oahu is
				// greater than 0, or if boat location is not at Oahu, or if the boat not at
				// Oahu,
				// have thread wait on Oahu, otherwise, send adult with boat to Molokai
				if (passengers > 0 || childrenOnOahu > 1 || boatLocation != Oahu) {
					// System.out.println(KThread.currentThread().getName() + " called to wait");
					waitingOnOahu.wake();
					waitingOnOahu.sleep();
				} else {
					// System.out.println(KThread.currentThread().getName() + " called to row to
					// Molokai");
					passengers = 1;
					adultsOnOahu--;
					bg.AdultRowToMolokai();
					boatLocation = Molokai;
					adultsOnMolokai++;
					location = Molokai;
					passengers = 0;
					waitingOnMolokai.wakeAll();
					waitingOnMolokai.sleep();
				}
			} else if (location == Molokai) {
				// System.out.println(KThread.currentThread().getName() + " called to wait on
				// Molokai");
				waitingOnMolokai.sleep();
			}
			boat.release();
		}
	}

	static void ChildItinerary(int location) {
		while (!complete) {
			boat.acquire();
			// Below used to debug to make sure threads are called correctly
			// System.out.println(KThread.currentThread().getName() + " called");
			if (location == Oahu) {
				// If the boat is already full or if the boat is not located at Oahu
				// have thread wait at Oahu
				if (passengers == 2 || boatLocation != Oahu) {
					// System.out.println(KThread.currentThread().getName() + " called to wait");
					waitingOnOahu.wake();
					waitingOnOahu.sleep();
				}
				// If there is only one child left on Oahu, then send child back to
				// Molokai and communicate that all threads are complete
				if (childrenOnOahu == 1 && adultsOnOahu == 0) {
					// System.out.println(KThread.currentThread().getName() + " called as last
					// child");
					passengers = 1;
					childrenOnOahu--;
					bg.ChildRowToMolokai();
					boatLocation = Molokai;
					location = Molokai;
					childrenOnMolokai++;
					passengers = 0;
					// System.out.printf("%d children on Molokai\n", childrenOnMolokai);
					done.speak(adultsOnMolokai + childrenOnMolokai);
					waitingOnMolokai.sleep();
				}
				// If there are enough children on Oahu and the number of
				// passengers is less than 2(the maximum number of passengers
				// that can be fit in the boat with children) increase number
				// of passengers by 1
				if (childrenOnOahu > 1 && passengers < 1 && boatLocation == Oahu) {
					// System.out.println(KThread.currentThread().getName() + " called onto boat");
					passengers++;
					waitingOnOahu.wake();
					riding.sleep();
					childrenOnOahu--;
					bg.ChildRideToMolokai();
					passengers = 0;
					location = Molokai;
					childrenOnMolokai++;
					done.speak(adultsOnMolokai + childrenOnMolokai);
					waitingOnMolokai.wakeAll();
					waitingOnMolokai.sleep();
				}
				// Wait for additional passenger to board, then row boat to Molokai
				if (passengers == 1 && boatLocation == Oahu) {
					// System.out.println(KThread.currentThread().getName() + " called as rower");
					passengers++;
					riding.wake();
					childrenOnOahu--;
					bg.ChildRowToMolokai();
					boatLocation = Molokai;
					location = Molokai;
					childrenOnMolokai++;
					riding.wake();
					waitingOnMolokai.sleep();
				}
			}
			// Send 1 child back to Oahu to return boat
			else if (location == Molokai && boatLocation == Molokai && passengers == 0 && !complete) {
				// System.out.println(KThread.currentThread().getName() + " called on Molokai");
				childrenOnMolokai--;
				bg.ChildRowToOahu();
				boatLocation = Oahu;
				location = Oahu;
				childrenOnOahu++;
				waitingOnOahu.wakeAll();
				waitingOnOahu.sleep();
			}
			boat.release();
		}
		// System.out.printf("broke out of loop\n");
	}

	/*
	 * static void SampleItinerary() { // Please note that this isn't a valid
	 * solution (you can't fit // all of them on the boat). Please also note that
	 * you may not // have a single thread calculate a solution and then just play
	 * // it back at the autograder -- you will be caught.
	 * 
	 * System.out.println("\n ***Everyone piles on the boat and goes to Molokai***"
	 * ); bg.AdultRowToMolokai(); bg.ChildRideToMolokai(); bg.AdultRideToMolokai();
	 * bg.ChildRideToMolokai(); }
	 */
}
