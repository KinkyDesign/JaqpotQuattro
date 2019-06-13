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
package org.jaqpot.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author pantelispanka
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Doa extends JaqpotEntity {
    
    private String modelId;
    private float[][] doaMatrix;
    private float aValue;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public float[][] getDoaMatrix() {
        return doaMatrix;
    }

    public void setDoaMatrix(float[][] doiMatrix) {
        this.doaMatrix = doiMatrix;
    }

    public float getaValue() {
        return aValue;
    }

    public void setaValue(float aValue) {
        this.aValue = aValue;
    }
    
}
