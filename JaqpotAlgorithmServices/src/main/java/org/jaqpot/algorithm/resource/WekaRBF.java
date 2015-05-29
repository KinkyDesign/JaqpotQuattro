/*
 *
 * JAQPOT Quattro
 *
 * JAQPOT Quattro and the components shipped with it, in particular:
 * (i)   JaqpotCoreServices
 * (ii)  JaqpotAlgorithmServices
 * (iii) JaqpotDB
 * (iv)  JaqpotDomain
 * (v)   JaqpotEAR
 * are licensed by GPL v3 as specified hereafter. Additional components may ship
 * with some other licence as will be specified therein.
 *
 * Copyright (C) 2014-2015 KinkyDesign (Charalampos Chomenidis, Pantelis Sopasakis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Source code:
 * The source code of JAQPOT Quattro is available on github at:
 * https://github.com/KinkyDesign/JaqpotQuattro
 * All source files of JAQPOT Quattro that are stored on github are licensed
 * with the aforementioned licence. 
 */
package org.jaqpot.algorithm.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jaqpot.algorithm.model.WekaModel;
import org.jaqpot.algorithm.weka.InstanceUtils;
import org.jaqpot.core.model.dto.jpdi.PredictionRequest;
import org.jaqpot.core.model.dto.jpdi.PredictionResponse;
import org.jaqpot.core.model.dto.jpdi.TrainingRequest;
import org.jaqpot.core.model.dto.jpdi.TrainingResponse;
import org.jaqpot.core.model.factory.ErrorReportFactory;
import weka.classifiers.Classifier;
import weka.classifiers.functions.RBFNetwork;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author hampos
 */
@Path("rbf")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WekaRBF {

    private final Integer _seed = 1,
            _maxIts = -1,
            _numClusters = 2;
    private final Double _minStdDev = 0.1, _ridge = 1.0e-8;

    @POST
    @Path("training")
    public Response training(TrainingRequest request) {

        try {
            if (request.getDataset().getDataEntry().isEmpty() || request.getDataset().getDataEntry().get(0).getValues().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ErrorReportFactory.badRequest("Dataset is empty", "Cannot train model on empty dataset"))
                        .build();
            }
            List<String> features = request.getDataset()
                    .getDataEntry()
                    .stream()
                    .findFirst()
                    .get()
                    .getValues()
                    .keySet()
                    .stream()
                    .collect(Collectors.toList());

            Instances data = InstanceUtils.createFromDataset(request.getDataset(), request.getPredictionFeature());

            Map<String, Object> parameters = request.getParameters() != null ? request.getParameters() : new HashMap<>();

            Double minStdDev = Double.parseDouble(parameters.getOrDefault("minStdDev", _minStdDev).toString());
            Double ridge = Double.parseDouble(parameters.getOrDefault("ridge", _ridge).toString());
            Integer seed = Integer.parseInt(parameters.getOrDefault("seed", _seed).toString());
            Integer maxIts = Integer.parseInt(parameters.getOrDefault("maxIts", _maxIts).toString());
            Integer numClusters = Integer.parseInt(parameters.getOrDefault("numClusters", _numClusters).toString());

            RBFNetwork rbf = new RBFNetwork();

            rbf.setMinStdDev(minStdDev);
            rbf.setRidge(ridge);
            rbf.setClusteringSeed(seed);
            rbf.setMaxIts(maxIts);
            rbf.setNumClusters(numClusters);

            rbf.buildClassifier(data);

            WekaModel model = new WekaModel();
            model.setClassifier(rbf);

            TrainingResponse response = new TrainingResponse();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(baos);
            out.writeObject(model);
            String base64Model = Base64.getEncoder().encodeToString(baos.toByteArray());
            response.setRawModel(base64Model);
            List<String> independentFeatures = features
                    .stream()
                    .filter(feature -> !feature.equals(request.getPredictionFeature()))
                    .collect(Collectors.toList());
            response.setIndependentFeatures(independentFeatures);
//            response.setPmmlModel(pmml);
            response.setAdditionalInfo(request.getPredictionFeature());
            response.setPredictedFeatures(Arrays.asList("Weka RBF prediction of " + request.getPredictionFeature()));

            return Response.ok(response).build();
        } catch (Exception ex) {
            Logger.getLogger(WekaMLR.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ErrorReportFactory.internalServerError()).build();
        }
    }

    @POST
    @Path("prediction")
    public Response prediction(PredictionRequest request) {

        try {
            if (request.getDataset().getDataEntry().isEmpty() || request.getDataset().getDataEntry().get(0).getValues().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ErrorReportFactory.badRequest("Dataset is empty", "Cannot train model on empty dataset"))
                        .build();
            }

            String base64Model = (String) request.getRawModel();
            byte[] modelBytes = Base64.getDecoder().decode(base64Model);
            ByteArrayInputStream bais = new ByteArrayInputStream(modelBytes);
            ObjectInput in = new ObjectInputStream(bais);
            WekaModel model = (WekaModel) in.readObject();

            Classifier classifier = model.getClassifier();
            Instances data = InstanceUtils.createFromDataset(request.getDataset());
            String dependentFeature = (String) request.getAdditionalInfo();
            data.insertAttributeAt(new Attribute(dependentFeature), data.numAttributes());
            data.setClass(data.attribute(dependentFeature));

            List<Map<String, Object>> predictions = new ArrayList<>();
//            data.stream().forEach(instance -> {
//                try {
//                    double prediction = classifier.classifyInstance(instance);
//                    Map<String, Object> predictionMap = new HashMap<>();
//                    predictionMap.put("Weka MLR prediction of " + dependentFeature, prediction);
//                    predictions.add(predictionMap);
//                } catch (Exception ex) {
//                    Logger.getLogger(WekaMLR.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            });

            for (int i = 0; i < data.numInstances(); i++) {
                Instance instance = data.instance(i);
                try {
                    double prediction = classifier.classifyInstance(instance);
                    Map<String, Object> predictionMap = new HashMap<>();
                    predictionMap.put("Weka RBF prediction of " + dependentFeature, prediction);
                    predictions.add(predictionMap);
                } catch (Exception ex) {
                    Logger.getLogger(WekaMLR.class.getName()).log(Level.SEVERE, null, ex);
                    return Response.status(Response.Status.BAD_REQUEST).entity(ErrorReportFactory.badRequest("Error while gettting predictions.", ex.getMessage())).build();
                }
            }

            PredictionResponse response = new PredictionResponse();
            response.setPredictions(predictions);
            return Response.ok(response).build();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(WekaMLR.class.getName()).log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorReportFactory.internalServerError())
                    .build();
        }
    }
}
