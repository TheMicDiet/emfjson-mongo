package org.emfjson.mongo.bson.codecs;

import org.bson.json.Converter;
import org.bson.json.StrictJsonWriter;

// fixes conversion from bson long to json
public class JsonInt64Converter implements Converter<Long> {
	@Override
	public void convert(final Long value, final StrictJsonWriter writer) {
		writer.writeNumber(Long.toString(value));
	}

}
