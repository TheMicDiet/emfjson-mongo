package org.emfjson.mongo.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory;
import org.emfjson.model.ModelPackage;
import org.emfjson.model.TestA;
import org.emfjson.model.TestB;
import org.emfjson.mongo.MongoHandler;
import org.emfjson.mongo.MongoOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadReferenceTest {

	private MongoClient client;
	private ResourceSetImpl resourceSet;
	private MongoHandler handler;
	private URI testURI = URI.createURI("mongodb://localhost:27018/test/models");

	@Before
	public void setUp() throws JsonProcessingException {
		client = MongoClients.create("mongodb://localhost:27018");

		handler = new MongoHandler(client);
		resourceSet = new ResourceSetImpl();

		resourceSet.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new JsonResourceFactory());
		resourceSet.getURIConverter().getURIHandlers().add(0, handler);

		Map<String, Object> options = new HashMap<>();
		options.put(MongoOptions.OPTION_REF_FIELD, "_ref");

		resourceSet.getLoadOptions().putAll(options);
	}

	private String uri(EClass eClass) {
		return EcoreUtil.getURI(eClass).toString();
	}

	@After
	public void tearDown() {
		client.getDatabase("test").drop();
	}

	protected void fixtureOne() throws JsonProcessingException {
		MongoCollection<Document> c1 = handler.getCollection(testURI.appendSegment("c1"));
		MongoCollection<Document> c2 = handler.getCollection(testURI.appendSegment("c2"));

		ObjectMapper mapper = new ObjectMapper();
		JsonNode content_c1 = mapper.createObjectNode().put("_id", "c1").put("_type", "resource").set("contents",
				mapper.createObjectNode().put("eClass", uri(ModelPackage.Literals.TEST_A)).set("oneB",
						mapper.createObjectNode().put("eClass", uri(ModelPackage.Literals.TEST_B)).put("_ref",
								"mongodb://localhost:27018/test/models/c2#/")));

		JsonNode content_c2 = mapper.createObjectNode().put("_id", "c2").put("_type", "resource").set("contents",
				mapper.createObjectNode().put("eClass", uri(ModelPackage.Literals.TEST_B)));

		c1.insertOne(Document.parse(mapper.writeValueAsString(content_c1)));
		c2.insertOne(Document.parse(mapper.writeValueAsString(content_c2)));
	}

	@Test
	public void testLoadingCrossReference() throws JsonProcessingException {
		fixtureOne();

		Resource c1 = resourceSet.getResource(testURI.appendSegment("c1"), true);

		TestA a = (TestA) c1.getContents().get(0);

		assertThat(a.getOneB()).isNotNull();

		Resource c2 = resourceSet.getResource(testURI.appendSegment("c2"), true);

		assertThat(c2.getContents()).hasSize(1);

		assertThat(a.getOneB()).isSameAs(c2.getContents().get(0));
	}

	protected void fixtureTwo() throws JsonProcessingException {
		MongoCollection<Document> c1 = handler.getCollection(testURI.appendSegment("c1"));
		MongoCollection<Document> c2 = handler.getCollection(testURI.appendSegment("c2"));
		MongoCollection<Document> c3 = handler.getCollection(testURI.appendSegment("c3"));

		ObjectMapper mapper = new ObjectMapper();
		JsonNode content_c1 = mapper.createObjectNode().put("_id", "c1").put("_type", "resource").set("contents",
				mapper.createObjectNode().put("eClass", uri(ModelPackage.Literals.TEST_A)).set("manyBs",
						mapper.createArrayNode()
								.add(mapper.createObjectNode().put("eClass", uri(ModelPackage.Literals.TEST_B))
										.put("_ref", "mongodb://localhost:27018/test/models/c2#/"))
								.add(mapper.createObjectNode().put("eClass", uri(ModelPackage.Literals.TEST_B))
										.put("_ref", "mongodb://localhost:27018/test/models/c3#/"))));

		JsonNode content_c2 = mapper.createObjectNode().put("_id", "c2").put("_type", "resource").set("contents",
				mapper.createObjectNode().put("eClass", uri(ModelPackage.Literals.TEST_B)));

		JsonNode content_c3 = mapper.createObjectNode().put("_id", "c3").put("_type", "resource").set("contents",
				mapper.createObjectNode().put("eClass", uri(ModelPackage.Literals.TEST_B)));

		c1.insertOne(Document.parse(mapper.writeValueAsString(content_c1)));
		c2.insertOne(Document.parse(mapper.writeValueAsString(content_c2)));
		c3.insertOne(Document.parse(mapper.writeValueAsString(content_c3)));
	}

	@Test
	public void testLoadingManyCrossReference() throws JsonProcessingException {
		fixtureTwo();

		Resource c1 = resourceSet.getResource(testURI.appendSegment("c1"), true);

		TestA a = (TestA) c1.getContents().get(0);

		assertThat(a.getManyBs()).hasSize(2);

		Resource c2 = resourceSet.getResource(testURI.appendSegment("c2"), true);
		Resource c3 = resourceSet.getResource(testURI.appendSegment("c3"), true);

		assertThat(c2.getContents()).hasSize(1);
		assertThat(c3.getContents()).hasSize(1);
		assertThat(a.getManyBs()).containsExactly((TestB) c2.getContents().get(0), (TestB) c3.getContents().get(0));
	}

}
