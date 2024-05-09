/* COMPSCI 424 Program 3
 * Name: Will Anthoney
 * 
 * This is a template. Program3.java *must* contain the main class
 * for this program. 
 * 
 * You will need to add other classes to complete the program, but
 * there's more than one way to do this. Create a class structure
 * that works for you. Add any classes, methods, and data structures
 * that you need to solve the problem and display your solution in the
 * correct format..
 */

package compsci424.p3;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Program3 {
    private static int numResources, numProcesses;
    private static int[] available;
    private static int[][] max, allocation, need;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Not enough command-line arguments provided, exiting.");
            return;
        }

        String mode = args[0];
        String path = args[1];
        
        // Display which mode and what file is being used.
        System.out.println("Selected mode: " + mode);
        System.out.println("Setup file location: " + path);

        // Reading the file
        try (BufferedReader setupFileReader = new BufferedReader(new FileReader(path))) {
            initializeFromSetupFile(setupFileReader);
            
            // Check initial conditions
            if (!checkInitialConditions()) {
                System.err.println("Initial conditions are not met, exiting.");
                return;
            }
            // Determining what mode is being used
            if (mode.equals("manual")) {
                runManualMode();
            } else if (mode.equals("auto")) {
                runAutoMode();
            } else { // Error handling
                System.err.println("Invalid mode. Please select either 'auto' or 'manual'.");
                return;
            }
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find setup file at " + args[1] + ", exiting.");
        } catch (IOException e) {
            System.err.println("Error reading setup file: " + e.getMessage());
        }
    }

    // Parsing the file and getting resources, processes, available, max, and allocation
    private static void initializeFromSetupFile(BufferedReader setupFileReader) throws IOException {
        String currentLine;

        // Read number of resources
        currentLine = setupFileReader.readLine();
        numResources = Integer.parseInt(currentLine.split(" ")[0]);

        // Read number of processes
        currentLine = setupFileReader.readLine();
        numProcesses = Integer.parseInt(currentLine.split(" ")[0]);

        // Initialize arrays
        available = new int[numResources];
        max = new int[numProcesses][numResources];
        allocation = new int[numProcesses][numResources];
        need = new int[numProcesses][numResources];

        // Read Available resources
        currentLine = setupFileReader.readLine(); // skip the "Available" line
        String[] availableValues = setupFileReader.readLine().split("\\s+");
        for (int i = 0; i < numResources; i++) {
            available[i] = Integer.parseInt(availableValues[i]);
        }

        // Read Max resources
        currentLine = setupFileReader.readLine(); // skip the "Max" line
        for (int i = 0; i < numProcesses; i++) {
            String[] maxValues = setupFileReader.readLine().split("\\s+");
            for (int j = 0; j < numResources; j++) {
                max[i][j] = Integer.parseInt(maxValues[j]);
            }
        }

        // Initialize Need array
        for (int i = 0; i < numProcesses; i++) {
            for (int j = 0; j < numResources; j++) {
                need[i][j] = max[i][j] - allocation[i][j];
            }
        }
    }

    private static boolean checkInitialConditions() {
        // Check if allocation is less than or equal to maximum for each process and resource
        for (int i = 0; i < numProcesses; i++) {
            for (int j = 0; j < numResources; j++) {
                if (allocation[i][j] > max[i][j]) {
                    return false;
                }
            }
        }
        return true; // If all conditions are met
    }

    private static void runAutoMode() {
        Random random = new Random();

        for (int i = 0; i < numProcesses; i++) {
            int processID = i;
            Thread thread = new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    // Generate random request
                    int resourceType = random.nextInt(numResources);
                    int unitsRequested = random.nextInt(max[processID][resourceType] + 1);

                    boolean granted = processResourceRequest(processID, resourceType, unitsRequested);
                    System.out.println("Thread " + processID + " requests " + unitsRequested + " units of resource " + resourceType + ": " + (granted ? "granted" : "denied"));

                    // Generate random release
                    resourceType = random.nextInt(numResources);
                    int unitsReleased = random.nextInt(allocation[processID][resourceType] + 1);

                    releaseResources(processID, resourceType, unitsReleased);
                    System.out.println("Thread " + processID + " releases " + unitsReleased + " units of resource " + resourceType);
                }
            });
            thread.start();
        }
    }
    
    private static void runManualMode() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.print("Enter a command (request/release/end): ");
            String command = scanner.nextLine();

            // Process user command
            if (command.startsWith("request")) {
                // Parse request command and process
                // Example: request 3 of 1 for 0
                String[] parts = command.split(" ");
                int unitsRequested = Integer.parseInt(parts[1]);
                int resourceType = Integer.parseInt(parts[3]);
                int processID = Integer.parseInt(parts[5]);

                boolean granted = processResourceRequest(processID, resourceType, unitsRequested);
                System.out.println("Thread " + processID + " requests " + unitsRequested + " units of resource " + resourceType + ": " + (granted ? "granted" : "denied"));
            } else if (command.startsWith("release")) {
                // Parse release command and process
                // Example: release 3 of 1 for 0
                String[] parts = command.split(" ");
                int unitsReleased = Integer.parseInt(parts[1]);
                int resourceType = Integer.parseInt(parts[3]);
                int processID = Integer.parseInt(parts[5]);

                releaseResources(processID, resourceType, unitsReleased);
                System.out.println("Thread " + processID + " releases " + unitsReleased + " units of resource " + resourceType);
            } else if (command.equals("end")) {
                running = false;
            } else {
                System.out.println("Invalid command. Try again.");
            }
        }
        scanner.close();
    }

    // Printing to user process resource requests, plus validations whether resources can be granted or not
    private static synchronized boolean processResourceRequest(int processID, int resourceType, int unitsRequested) {
        System.out.println("Processing request: Thread " + processID + " requests " + unitsRequested + " units of resource " + resourceType);
        System.out.println("Available resources before request: " + Arrays.toString(available));
        System.out.println("Need matrix before request:");
        printMatrix(need);
        System.out.println("Allocation matrix before request:");
        printMatrix(allocation);

        // Check if there's enough available resources
        if (unitsRequested <= available[resourceType]) {
            // Check if the request does not exceed the maximum need and the maximum resource demand for that process
            if (unitsRequested <= need[processID][resourceType] && unitsRequested <= (max[processID][resourceType] - allocation[processID][resourceType])) {
                // Simulate granting the resource request
                int[] tempAvailable = available.clone();
                int[][] tempAllocation = new int[numProcesses][numResources];
                for (int i = 0; i < numProcesses; i++) {
                    tempAllocation[i] = allocation[i].clone();
                }

                tempAvailable[resourceType] -= unitsRequested;
                tempAllocation[processID][resourceType] += unitsRequested;

                // Updating the Need matrix
                for (int j = 0; j < numResources; j++) {
                    need[processID][j] = max[processID][j] - tempAllocation[processID][j];
                }

                // Check if the system remains in a safe state
                if (isSafeState(tempAvailable, tempAllocation)) {
                    // Grant the resource request
                    allocation[processID][resourceType] += unitsRequested;
                    available[resourceType] -= unitsRequested;
                    need[processID][resourceType] -= unitsRequested;

                    System.out.println("Request granted");
                    System.out.println("Available resources after request: " + Arrays.toString(available));
                    System.out.println("Need matrix after request:");
                    printMatrix(need);
                    System.out.println("Allocation matrix after request:");
                    printMatrix(allocation);

                    return true;
                } else {
                    System.out.println("Request denied: Unsafe state after granting the request");
                }
            } else {
                System.out.println("Request denied: Exceeds need or maximum resource demand for process");
            }
        } else {
            System.out.println("Request denied: Not enough available resources");
        }

        return false;
    }

    // Method for printing matrices
    private static void printMatrix(int[][] matrix) {
        for (int i = 0; i < numProcesses; i++) {
            System.out.println(Arrays.toString(matrix[i]));
        }
    }

    // Resource release method
    private static synchronized void releaseResources(int processID, int resourceType, int unitsReleased) {
        allocation[processID][resourceType] -= unitsReleased;
        available[resourceType] += unitsReleased;

        // Update the Need matrix when resources are released
        need[processID][resourceType] += unitsReleased;

        System.out.println("Thread " + processID + " releases " + unitsReleased + " units of resource " + resourceType);
    }

    // Checks if there are enough resources to be "safe"
    private static boolean isSafeState(int[] tempAvailable, int[][] tempAllocation) {
        // Array to track if a process can complete its execution
        boolean[] canFinish = new boolean[numProcesses];
        Arrays.fill(canFinish, false);

        // Work array to represent the available resources
        int[] work = tempAvailable.clone();

        // Check if there are enough available resources for each process to complete
        for (int i = 0; i < numProcesses; i++) {
            if (!canFinish[i]) {
                boolean canAllocate = true;
                for (int j = 0; j < numResources; j++) {
                    if (tempAllocation[i][j] > work[j]) {
                        canAllocate = false;
                        break;
                    }
                }
                if (canAllocate) {
                    // Process i can finish its execution
                    canFinish[i] = true;
                    // Release allocated resources by process i
                    for (int j = 0; j < numResources; j++) {
                        work[j] += tempAllocation[i][j];
                    }
                    // Check if other processes can now finish
                    i = -1; // Reset i to check from the beginning
                }
            }
        }

        // Check if all processes can finish
        for (boolean finish : canFinish) {
            if (!finish) {
                return false; // System is not in a safe state
            }
        }
        return true; // System is in a safe state
    }
}
