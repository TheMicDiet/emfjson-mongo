package org.emfjson.mongo.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter.Loadable;
import org.eclipse.emfcloud.jackson.databind.EMFContext;
import org.eclipse.emfcloud.jackson.module.EMFModule;
import org.emfjson.mongo.MongoHandler;
import org.emfjson.mongo.MongoOptions;
import org.emfjson.mongo.bson.codecs.JsonInt64Converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

public class MongoInputStream extends InputStream implements Loadable {

	private final Map<?, ?> options;
	private final URI uri;
	private MongoHandler handler;

	public MongoInputStream(MongoHandler handler, URI uri, Map<?, ?> options) {
		this.uri = uri;
		this.options = options;
		this.handler = handler;
	}

	@Override
	public void loadResource(Resource resource) throws IOException {
		final MongoCollection<Document> collection = handler.getCollection(uri);

		if (!resource.getContents().isEmpty()) {
			resource.getContents().clear();
		}

		final String id = uri.segment(2);
		final Document filter = new Document(MongoHandler.ID_FIELD, id);
		final Document document = collection.find(filter).limit(1).first();

		if (document == null) {
			throw new IOException("Cannot find document with " + MongoHandler.ID_FIELD + " " + id);
		}
		
		JsonWriterSettings settings = JsonWriterSettings.builder().int64Converter(new JsonInt64Converter()).build();
		JsonWriter writer = new JsonWriter(new StringWriter(), settings);
		
		new DocumentCodec().encode(writer, document,
				EncoderContext.builder().isEncodingCollectibleDocument(true).build());
		readJson(resource, writer.getWriter().toString());
	}

	private void readJson(Resource resource, String data) throws IOException {
		final ObjectMapper mapper = new ObjectMapper();

		final EMFModule module = new EMFModule();
		MongoOptions.configureEMFModule(module, options);
		mapper.registerModule(module);

		final JsonNode rootNode = mapper.readTree(data);
		final JsonNode contents = rootNode.has(MongoHandler.CONTENTS_FIELD) ? rootNode.get(MongoHandler.CONTENTS_FIELD)
				: null;

		if (contents != null) {
			mapper.reader().withAttribute(EMFContext.Attributes.RESOURCE_SET, resource.getResourceSet())
					.withAttribute(EMFContext.Attributes.RESOURCE, resource).treeToValue(contents, Resource.class);
		}
	}

	@Override
	public int read() throws IOException {
		return 0;
	}

}
