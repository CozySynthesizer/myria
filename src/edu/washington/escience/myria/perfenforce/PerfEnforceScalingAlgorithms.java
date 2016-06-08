/**
 *
 */
package edu.washington.escience.myria.perfenforce;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;

import edu.washington.escience.myria.perfenforce.encoding.InitializeScalingEncoding;
import edu.washington.escience.myria.perfenforce.encoding.ScalingAlgorithmEncoding;
import edu.washington.escience.myria.perfenforce.encoding.ScalingStatusEncoding;

/**
 * 
 */
public class PerfEnforceScalingAlgorithms {

  int tier;
  int ithQuerySequence;
  int currentClusterSize;
  List<Integer> configs;
  QueryMetaData currentQuery;

  String path;

  ScalingAlgorithm scalingAlgorithm;

  protected static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PerfEnforceScalingAlgorithms.class);

  public PerfEnforceScalingAlgorithms(final InitializeScalingEncoding scalingEncoding) {
    tier = scalingEncoding.tier;
    path = scalingEncoding.path;

    configs = Arrays.asList(4, 6, 8, 10, 12);
    ithQuerySequence = 0;
    currentClusterSize = configs.get(tier);

    initializeScalingAlgorithm(scalingEncoding);
  }

  public void initializeScalingAlgorithm(final InitializeScalingEncoding scalingEncoding) {
    switch (scalingEncoding.scalingAlgorithm.name) {
      case "RL":
        scalingAlgorithm =
            new ReinforcementLearning(currentClusterSize, scalingEncoding.scalingAlgorithm.alpha,
                scalingEncoding.scalingAlgorithm.beta);
        break;
      case "PI":
        break;
      case "OML":
        break;
    }
  }

  public int getCurrentClusterSize() {
    return currentClusterSize;
  }

  public int getCurrentQueryIdealSize() {
    return currentQuery.idealClusterSize;
  }

  public int getQueryCounter() {
    return ithQuerySequence;
  }

  public void setCurrentQuery(final QueryMetaData currentQuery) {
    this.currentQuery = currentQuery;
  }

  public void incrementQueryCounter() {
    ithQuerySequence++;
  }

  public void step() {
    scalingAlgorithm.step();
    currentClusterSize = scalingAlgorithm.getCurrentClusterSize();
  }

  public void setupNextFakeQuery() {
    // Files to read
    String queryRuntimeFile = path + "query_runtimes";
    LOGGER.warn(queryRuntimeFile);
    String slaFile = path + "SLAs/tier" + tier;
    LOGGER.warn(slaFile);
    String idealFile = path + "Ideal/ideal" + tier;
    LOGGER.warn(idealFile);

    try {
      BufferedReader runtimeReader = new BufferedReader(new InputStreamReader(new FileInputStream(queryRuntimeFile)));
      BufferedReader slaReader = new BufferedReader(new InputStreamReader(new FileInputStream(slaFile)));
      BufferedReader idealReader = new BufferedReader(new InputStreamReader(new FileInputStream(idealFile)));

      String runtimeLine = runtimeReader.readLine();
      String slaLine = slaReader.readLine();
      String idealLine = idealReader.readLine();
      int counter = 0;
      while (runtimeLine != null) {
        if (counter == ithQuerySequence) {
          String[] runtimeParts = runtimeLine.split(",");
          List<Integer> queryRuntimesList =
              Arrays.asList(Integer.parseInt(runtimeParts[0]), Integer.parseInt(runtimeParts[1]), Integer
                  .parseInt(runtimeParts[2]), Integer.parseInt(runtimeParts[3]), Integer.parseInt(runtimeParts[4]));
          int queryIdeal = Integer.parseInt(idealLine);
          double querySLA = Double.parseDouble(slaLine);
          QueryMetaData q = new QueryMetaData(counter, querySLA, queryIdeal, queryRuntimesList);
          setCurrentQuery(q);
        }

        runtimeLine = runtimeReader.readLine();
        slaLine = slaReader.readLine();
        idealLine = idealReader.readLine();
        counter++;
      }
      runtimeReader.close();
      slaReader.close();
      idealReader.close();

    } catch (NumberFormatException | IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param scalingAlgorithmEncoding
   */
  public void updateParameters(final ScalingAlgorithmEncoding scalingAlgorithmEncoding) {
    if (scalingAlgorithm instanceof ReinforcementLearning) {
      ReinforcementLearning r = (ReinforcementLearning) scalingAlgorithm;
      r.setAlpha(scalingAlgorithmEncoding.alpha);
      r.setBeta(scalingAlgorithmEncoding.beta);
    } else if (scalingAlgorithm instanceof PIControl) {
      PIControl p = (PIControl) scalingAlgorithm;
      p.setKP(scalingAlgorithmEncoding.kp);
      p.setKI(scalingAlgorithmEncoding.ki);
    } else if (scalingAlgorithm instanceof OnlineMachineLearning) {
      OnlineMachineLearning o = (OnlineMachineLearning) scalingAlgorithm;
      o.setLR(scalingAlgorithmEncoding.lr);
    }
  }

  /**
   */
  public ScalingStatusEncoding getScalingStatus() {
    return scalingAlgorithm.getScalingStatus();
  }

}
