import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PSO {

    private int bestNum;
    private float inertiaWeight;
    private int MAX_GEN;// iteration time
    private int numParticles;// particle num

    private int pointNum; // point num
    private int t;// current generation

    private int begin;// start point

    private int[][] distance; // matrix of distance

    private int[][] particleSwarm;// particle swarm
    private ArrayList<ArrayList<SO>> listV;// swap list of each particle

    private int[][] pbest;// best solution of each particle among all generations
    private int[] vpbest;// evaluation value of best solution

    private int[] gbest;// global best solution

    public int[] getgbest() {

        return gbest;
    }

    public int getvgbest() {

        return vgbest;
    }

    private int vgbest;// evaluation value of global best solution
    private int bestT;// best generation

    private int[] fitness;

    private Random random;

    ExecutorService executorService = Executors.newFixedThreadPool(50);

    public PSO(int cityNum, int g, int s, float w, int b) {
        this.pointNum = cityNum;
        this.MAX_GEN = g;
        this.numParticles = s;
        this.inertiaWeight = w;
        this.begin = b;
    }

    public void initialize(String filename) throws IOException {
        int[] x;
        int[] y;
        String strbuff;
        BufferedReader data = new BufferedReader(new InputStreamReader(
                new FileInputStream(filename)));
        distance = new int[pointNum][pointNum];
        x = new int[pointNum];
        y = new int[pointNum];

        distance[pointNum - 1][pointNum - 1] = 0;

        particleSwarm = new int[numParticles][pointNum];
        fitness = new int[numParticles];

        // individual
        pbest= new int[numParticles][pointNum];
        vpbest= new int[numParticles];

        // global
        gbest = new int[pointNum];
        vgbest = Integer.MAX_VALUE;

        bestT = 0;
        t = 0;

        random = new Random(System.currentTimeMillis());

        for (int i = 0; i < pointNum; i++) {
            strbuff = data.readLine();
            String[] strcol = strbuff.split(" ");
            x[i] = Integer.valueOf(strcol[1]);// x
            y[i] = Integer.valueOf(strcol[2]);// y
        }

        // calculate distance between points
        for (int i = 0; i < pointNum - 1; i++) {
            distance[i][i] = 0; 
            for (int j = i + 1; j < pointNum; j++) {
                double rij = Math.sqrt(((x[i] - x[j]) * (x[i] - x[j]) + (y[i] - y[j]) * (y[i] - y[j])) / 10.0);
                int tij = (int) Math.round(rij);
                if (tij < rij) {
                    distance[i][j] = tij + 1;
                    distance[j][i] = distance[i][j];
                } else {
                    distance[i][j] = tij;
                    distance[j][i] = distance[i][j];
                }
            }
        }
    }

    // initialize particle swarm
    void initializeSwarm() {
        int i, j, k;
        for (k = 0; k < numParticles; k++) // swarm num
        {
            // start point
            particleSwarm[k][0] = begin;
            for (i = 1; i < pointNum; ) // particle num
            {
                particleSwarm[k][i] = random.nextInt(65535) % pointNum;
                for (j = 0; j < i; j++) {
                    if (particleSwarm[k][i] == particleSwarm[k][j] || particleSwarm[k][i] == begin) {
                        break;
                    }
                }
                if (j == i) {
                    i++;
                }
            }
        }
    }

    // initialize swapping list of each particle
    void initializeListV() {
        int ra;
        int ra1;
        int ra2;

        listV = new ArrayList<ArrayList<SO>>();

        for (int i = 0; i < numParticles; i++) {
            ArrayList<SO> list = new ArrayList<SO>();
            ra = random.nextInt(65535) % pointNum;
            for (int j = 0; j < ra; j++) {
                ra1 = random.nextInt(65535) % pointNum;
                while (ra1 == 0) {
                    ra1 = random.nextInt(65535) % pointNum;
                }
                ra2 = random.nextInt(65535) % pointNum;
                while (ra1 == ra2 || ra2 == 0) {
                    ra2 = random.nextInt(65535) % pointNum;
                }

                SO S = new SO(ra1, ra2);
                list.add(S);
            }

            listV.add(list);
        }
    }

    public int evaluateLength(int[] chromosome) {
        int len = 0;
        // point 1, 2, 3...
        for (int i = 1; i < pointNum; i++) {
            len += distance[chromosome[i - 1]][chromosome[i]];
        }
        len += distance[chromosome[pointNum - 1]][chromosome[0]];
        return len;
    }

    public void add(int[] arr, ArrayList<SO> list) {
        int temp = 0;
        SO S;
        for (int i = 0; i < list.size(); i++) {
            S = list.get(i);
            temp = arr[S.getX()];
            arr[S.getX()] = arr[S.getY()];
            arr[S.getY()] = temp;
        }
    }

    // get swapping list from b to a
    public ArrayList<SO> minus(int[] a, int[] b) {
        int[] temp = b.clone();
        int index;
        // swapping unit
        SO S;
        // swapping list
        ArrayList<SO> list = new ArrayList<SO>();
        for (int i = 0; i < pointNum; i++) {
            if (a[i] != temp[i]) {
                // find the same index as a[i] in temp[]
                index = findNumIndex(temp, a[i]);
                // change i and index in temp[]
                changeIndex(temp, i, index);
                // record swapping unit
                S = new SO(i, index);
                // save swapping unit
                list.add(S);
            }
        }
        return list;
    }

    public int findNumIndex(int[] arr, int num) {
        int index = -1;
        for (int i = 0; i < pointNum; i++) {
            if (arr[i] == num) {
                index = i;
                break;
            }
        }
        return index;
    }

    public void changeIndex(int[] arr, int index1, int index2) {
        int temp = arr[index1];
        arr[index1] = arr[index2];
        arr[index2] = temp;
    }

    // ??????????????????
    public void copyArray(int[][] from, int[][] to) {
        for (int i = 0; i < numParticles; i++) {
            for (int j = 0; j < pointNum; j++) {
                to[i][j] = from[i][j];
            }
        }
    }

    // ??????????????????
    public void copyArrayNum(int[] from, int[] to) {
        for (int i = 0; i < pointNum; i++) {
            to[i] = from[i];
        }
    }

    private void updateParticle(int i) {
        ArrayList<SO> oldVelocityList;
        int len;
        int j;
        float ra;
        float rb;
        ArrayList<SO> newVelocityList = new ArrayList<SO>();

        // refresh velocity
        // newVelocityList=wVi+ra(Pid-Xid)+rb(gbest-Xid)
        oldVelocityList = listV.get(i);

        // wVi+????????????Vi???size*w?????????????????????
        len = (int) (oldVelocityList.size() * inertiaWeight);

        for (j = 0; j < len; j++) {
            newVelocityList.add(oldVelocityList.get(j));
        }

        // Pid-Xid
        ArrayList<SO> a = minus(pbest[i], particleSwarm[i]);
        ra = random.nextFloat();

        // ra(Pid-Xid)
        len = (int) (a.size() * ra);

        for (j = 0; j < len; j++) {
            newVelocityList.add(a.get(j));
        }

        // gbest-Xid
        ArrayList<SO> b = minus(gbest, particleSwarm[i]);
        rb = random.nextFloat();

        // rb(gbest-Xid)
        len = (int) (b.size() * rb);

        for (j = 0; j < len; j++) {
            SO tt = b.get(j);
            newVelocityList.add(tt);
        }

        // save new newVelocityList
        listV.set(i, newVelocityList);

        // refresh position
        // Xid???=Xid+Vid
        add(particleSwarm[i], newVelocityList);
    }

    public void evolveSwarm() {
        int i, k;
        for (t = 0; t < MAX_GEN; t++) {
            // create concurrent threads to record particles' movement
            ArrayList<Callable<Void>> runnables = new ArrayList<>();
            for (i = 0; i < numParticles; i++) {
                if (i == bestNum) continue;

                final int bestIndex = i;
                runnables.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        updateParticle(bestIndex);
                        return null;
                    }
                });

            }
            try {
                List<Future<Void>> futures = executorService.invokeAll(runnables);
                for (Future<Void> future : futures) {
                    future.get();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            // calculate fitness value of new swarm, get best solution
            for (k = 0; k < numParticles; k++) {
                fitness[k] = evaluateLength(particleSwarm[k]);
                if (vpbest[k] > fitness[k]) {
                    vpbest[k] = fitness[k];
                    copyArrayNum(particleSwarm[k], pbest[k]);
                    bestNum = k;
                }
                if (vgbest > vpbest[k]) {
                    System.out.println("Shortest distance: " + vgbest + " Generation: " + bestT);
                    bestT = t;
                    vgbest = vpbest[k];
                    copyArrayNum(pbest[k], gbest);
                }
            }
        }
    }

    public void solve() {
        int i;
        int k;

        initializeSwarm();
        initializeListV();

        // make each particle remember its own best solution
        copyArray(particleSwarm, pbest);

        for (k = 0; k < numParticles; k++) {
            fitness[k] = evaluateLength(particleSwarm[k]);
            vpbest[k] = fitness[k];
            if (vgbest > vpbest[k]) {
                vgbest = vpbest[k];
                copyArrayNum(pbest[k], gbest);
                bestNum = k;
            }
        }

        System.out.println("Initial particle swarm...");
        for (k = 0; k < numParticles; k++) {
            for (i = 0; i < pointNum; i++) {
                System.out.print(particleSwarm[k][i] + ",");
            }
            System.out.println();
            System.out.println("----" + fitness[k]);
        }

        evolveSwarm();

        System.out.println("Final particle swarm...");
        for (k = 0; k < numParticles; k++) {
            for (i = 0; i < pointNum; i++) {
                System.out.print(particleSwarm[k][i] + ",");
            }
            System.out.println();
            System.out.println("----" + fitness[k]);
        }

        System.out.println("Best generation: ");
        System.out.println(bestT);
        System.out.println("Shortest distance: ");
        System.out.println(vgbest);
        System.out.println("Best path: ");
        for (i = 0; i < pointNum; i++) {
            System.out.print(gbest[i] + ",");
        }

    }
    public static void main(String[] args) throws IOException {
        int pointNum = 14;

        // set the path of data file
        String tspData = "att48.txt";
        
        //String tspData = Files.readString(Paths.get("att48.txt"));
        //System.out.println(tspData);
        //int[] bestTour; // best path
        //int bestLength; // shortest length
        int[] x = new int[pointNum]; // matrix of X
        int[] y = new int[pointNum]; // matrix of Y
        

        try {
            x = ReadFile.getX(pointNum, tspData);
            y = ReadFile.getY(pointNum, tspData);
            for (int i = 0; i < pointNum; i++) {
                x[i] += 30;
                y[i] += 200;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int particleNum = 1000;
        int generation = 10000;
        float weight = (float)0.7;
        int beta = 2;

        PSO pso = new PSO(pointNum, generation, particleNum, weight, beta-1);
        pso.initialize(tspData);
        pso.solve();
    }

}
