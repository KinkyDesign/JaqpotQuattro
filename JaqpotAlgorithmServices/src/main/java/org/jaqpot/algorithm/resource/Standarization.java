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
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
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
import org.apache.commons.math3.stat.StatUtils;
import org.jaqpot.algorithm.model.ScalingModel;
import org.jaqpot.core.model.dto.jpdi.PredictionRequest;
import org.jaqpot.core.model.dto.jpdi.PredictionResponse;
import org.jaqpot.core.model.dto.jpdi.TrainingRequest;
import org.jaqpot.core.model.dto.jpdi.TrainingResponse;
import org.jaqpot.core.model.factory.ErrorReportFactory;

/**
 *
 * @author Charalampos Chomenidis
 * @author Pantelis Sopasakis
 */
@Path("std")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Standarization {

    private static final Logger LOG = Logger.getLogger(Standarization.class.getName());

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
                    .filter(feature -> !feature.equals(request.getPredictionFeature()))
                    .collect(Collectors.toList());

            Map<String, Double> maxValues = new HashMap<>();
            Map<String, Double> minValues = new HashMap<>();

            features.parallelStream().forEach(feature -> {
                List<Double> values = request.getDataset().getDataEntry().stream().map(dataEntry -> {
                    return Double.parseDouble(dataEntry.getValues().get(feature).toString());
                }).collect(Collectors.toList());
                double[] doubleValues = values.stream().mapToDouble(Double::doubleValue).toArray();

                Double mean = StatUtils.mean(doubleValues);
                Double stddev = Math.sqrt(StatUtils.variance(doubleValues));

                maxValues.put(feature, stddev);
                minValues.put(feature, mean);
            });
            ScalingModel model = new ScalingModel();
            model.setMaxValues(maxValues);
            model.setMinValues(minValues);

            TrainingResponse response = new TrainingResponse();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(baos);
            out.writeObject(model);
            String base64Model = Base64.getEncoder().encodeToString(baos.toByteArray());
            response.setRawModel(base64Model);
            response.setIndependentFeatures(features);
            response.setPredictedFeatures(features.stream().map(feature -> {
                return "Standarized " + feature;
            }).collect(Collectors.toList()));
            return Response.ok(response).build();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
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
            List<String> features = request.getDataset()
                    .getDataEntry()
                    .stream()
                    .findFirst()
                    .get()
                    .getValues()
                    .keySet()
                    .stream()
                    .collect(Collectors.toList());
            String base64Model = (String) request.getRawModel();
            byte[] modelBytes = Base64.getDecoder().decode(base64Model);
            ByteArrayInputStream bais = new ByteArrayInputStream(modelBytes);
            ObjectInput in = new ObjectInputStream(bais);
            ScalingModel model = (ScalingModel) in.readObject();
            in.close();
            bais.close();

            List<Map<String, Object>> predictions = new ArrayList<>();

            request.getDataset().getDataEntry().stream().forEach(dataEntry -> {
                Map<String, Object> data = new HashMap<>();
                features.stream().forEach(feature -> {
                    Double stdev = model.getMaxValues().get(feature);
                    Double mean = model.getMinValues().get(feature);
                    Double value = Double.parseDouble(dataEntry.getValues().get(feature).toString());
                    if (stdev != null && stdev != 0.0) {
                        value = (value - mean) / stdev;
                    } else {
                        value = 1.0;
                    }
                    data.put("Standarized " + feature, value);
                });
                predictions.add(data);
            });

            PredictionResponse response = new PredictionResponse();
            response.setPredictions(predictions);

            return Response.ok(response).build();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }
}
