package org.emfjson.mongo;

import java.util.Map;

import org.eclipse.emfcloud.jackson.annotations.EcoreIdentityInfo;
import org.eclipse.emfcloud.jackson.annotations.EcoreReferenceInfo;
import org.eclipse.emfcloud.jackson.module.EMFModule;

public class MongoOptions {
	
	// use ids for serialization
	public static final String OPTION_USE_ID = "OPTION_USE_ID";
	// set a custom name for the reference field during serialization
	public static final String OPTION_REF_FIELD = "OPTION_REF_FIELD";
	
	
	
	public static void configureEMFModule(EMFModule module, Map<?,?> options) {
		
		if(options.containsKey(OPTION_USE_ID)) {
			module.configure(EMFModule.Feature.OPTION_USE_ID, (boolean) options.get(OPTION_USE_ID));
			// set if field name
			module.setIdentityInfo(new EcoreIdentityInfo("_id"));
		}
		
		if(options.containsKey(OPTION_REF_FIELD)) {
			module.setReferenceInfo(new EcoreReferenceInfo((String) options.get(OPTION_REF_FIELD)));
		}
		
		
		
		
	}

}
