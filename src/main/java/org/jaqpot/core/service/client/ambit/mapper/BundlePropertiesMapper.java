package org.jaqpot.core.service.client.ambit.mapper;

/**
 * Created by Angelos Valsamis on 20/12/2016.
 */

import org.jaqpot.core.model.dto.bundle.BundleProperties;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Created by Angelos Valsamis on 12/12/2016.
 */
@Mapper
public interface BundlePropertiesMapper {
    BundlePropertiesMapper INSTANCE = Mappers.getMapper( BundlePropertiesMapper.class );

    BundleProperties bundlePropertiesToBundlePropertiesMapper (org.jaqpot.ambitclient.model.dto.bundle.BundleProperties bundleProperties);


}

