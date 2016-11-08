package ontology;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.FileManager;

/**
 * Provides an API for an ontology in OWL format. Allows adding/deleting instances and properties.
 * 
 * @author themis
 */
public class OntologyJenaAPI {

	/** The filename where the ontology resides. */
	private String filename;

	/** The namespace of the ontology. */
	private String NS;

	/** The base URI of the ontology. */
	private OntModel base;

	/**
	 * Initializes the connection of this API with the ontology. Upon calling this function, the ontology is loaded in
	 * memory. <u><b>NOTE</b></u> that you have to call {@link #close()} in order to save your changes to disk.
	 * 
	 * @param filename the filename where the ontology resides.
	 * @param source the source URI of the ontology.
	 */
	public OntologyJenaAPI(String filename, String source) {
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		this.filename = filename;
		NS = source + "#";
		base = ModelFactory.createOntologyModel();

		File f = new File(filename);
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		InputStream in = FileManager.get().open(filename);
		if (in == null) {
			throw new IllegalArgumentException("File: " + filename + " not found");
		}

		base.read(in, null);

		try {
			in.close();
		} catch (IOException e) {
		}
	}

	/**
	 * Adds the namespace prefix to an instance string.
	 * 
	 * @param instance the string name of the instance.
	 * @return the instance string with the namespace.
	 */
	private String addNamespaceToInstance(String instance) {
		if (!instance.contains(NS))
			instance = NS + instance.replaceAll("[^A-Za-z0-9]", "_");
		return instance;
	}

	/**
	 * Adds a new individual in the ontology.
	 * 
	 * @param className the OWL class that the new individual shall exist.
	 * @param individualName the name of the new individual to be added.
	 */
	public void addIndividual(String className, String individualName) {
		OntClass ontClass = base.getOntClass(addNamespaceToInstance(className));
		base.createIndividual(addNamespaceToInstance(individualName), ontClass);
	}

	/**
	 * Removes an individual of the ontology given its name. Note that this method removes also all the statements that
	 * refer to this individual.
	 * 
	 * @param individualName the name of the individual to be deleted.
	 */
	public void removeIndividual(String individualName) {
		getIndividual(individualName).remove();
	}

	/**
	 * Removes a property of a specific individual.
	 * 
	 * @param individualName the name of the individual of which the property is removed
	 * @param propertyName the name of the property of which the value is removed.
	 */
	public void removePropertyFromIndividual(String individualName, String propertyName) {
		(getIndividual(individualName).getProperty(getProperty(propertyName))).remove();
	}

	/**
	 * Removes all the individuals that connect to an individual given a specific property.
	 * 
	 * @param individualName the individual to which the individuals to be removed are connected.
	 * @param propertyName the name of the property that connects the individual with the individuals to be removed.
	 */
	public void removeIndividualsGivenIndividualAndProperty(String individualName, String propertyName) {
		Individual individual = getIndividual(individualName);
		Property property = getProperty(propertyName);
		ArrayList<Individual> individuals = new ArrayList<Individual>();
		for (NodeIterator propertiesIterator = individual.listPropertyValues(property); propertiesIterator.hasNext();)
			individuals.add(getIndividual(propertiesIterator.next().toString()));
		for (Iterator<Individual> individualIterator = individuals.iterator(); individualIterator.hasNext();)
			((Individual) individualIterator.next()).remove();
	}

	/**
	 * Returns an individual given its name.
	 * 
	 * @param individualName the name of the individual to be returned.
	 * @return the existing individual.
	 */
	private Individual getIndividual(String individualName) {
		return base.getIndividual(addNamespaceToInstance(individualName));
	}

	/**
	 * Returns a property given its name.
	 * 
	 * @param propertyName the name of the property to be returned.
	 * @return the existing property.
	 */
	private Property getProperty(String propertyName) {
		return base.getProperty(addNamespaceToInstance(propertyName));
	}

	/**
	 * Connects two individuals using a property.
	 * 
	 * @param individualName1 the first individual from which the property starts.
	 * @param propertyName the name of the property to connect the two individuals.
	 * @param individualName2 the second individual to which the property ends up.
	 */
	public void addPropertyBetweenIndividuals(String individualName1, String propertyName, String individualName2) {
		Property property = getProperty(propertyName);
		Individual individual1 = getIndividual(individualName1);
		Individual individual2 = getIndividual(individualName2);
		Statement stm = base.createStatement(individual1, property, individual2);
		base.add(stm);
	}

	/**
	 * Adds a property value to a property of an individual.
	 * 
	 * @param individualName the name of the individual.
	 * @param propertyName the name of the property to add the value to.
	 * @param propertyValue the value of the property to be added.
	 */
	public void addPropertyToIndividual(String individualName, String propertyName, String propertyValue) {
		Property property = getProperty(propertyName);
		Individual individual = getIndividual(individualName);
		Statement stm = base.createStatement(individual, property, propertyValue);
		base.add(stm);
	}

	/**
	 * Adds a float property value to a property of an individual.
	 * 
	 * @param individualName the name of the individual.
	 * @param propertyName the name of the property to add the value to.
	 * @param propertyValue the float value of the property to be added.
	 */
	public void addPropertyToIndividual(String individualName, String propertyName, float propertyValue) {
		Property property = getProperty(propertyName);
		Individual individual = getIndividual(individualName);
		Statement stm = base.createLiteralStatement(individual, property, propertyValue);
		base.add(stm);
	}

	/**
	 * Returns the names of the individuals that connect to the given individual ({@code individualName}) via the
	 * property given as parameter ({@code propertyName}).
	 * 
	 * @param individualName the name of the individual of which to find the connected individuals to.
	 * @param propertyName the property that connects the given individual to the returned individual names.
	 * @return an {@link ArrayList} containing the individual names.
	 */
	public ArrayList<String> getIndividualNamesGivenIndividualAndProperty(String individualName, String propertyName) {
		Individual individual = getIndividual(individualName);
		Property property = getProperty(propertyName);
		ArrayList<String> individualNames = new ArrayList<String>();
		for (NodeIterator individualsIterator = individual.listPropertyValues(property); individualsIterator.hasNext();)
			individualNames.add(((OntResource) individualsIterator.next()).getLocalName());
		return individualNames;
	}

	/**
	 * Returns the value of the property of an individual given its name and the name of the property.
	 * 
	 * @param individualName the name of the individual of which the property value is returned,
	 * @param propertyName the name of the property of which the value is returned,
	 * @return a {@link Literal} containing the returned property value.
	 */
	public Literal getIndividualPropertyValue(String individualName, String propertyName) {
		Individual individual = getIndividual(individualName);
		Property property = getProperty(propertyName);
		return (Literal) individual.getPropertyValue(property);
	}

	/**
	 * Performs a query on the ontology. The query is a string given in SPARQL format.
	 * 
	 * @param queryString the query string in SPARQL format.
	 * @return an object of type {@link com.hp.hpl.jena.query.ResultSet ResultSet} that contains the results.
	 */
	public ResultSet performQuery(String queryString) {
		Query query = QueryFactory.create(queryString);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, base);
		ResultSet results = queryExecution.execSelect();
		return results;
	}

	/**
	 * Closes the connection of the ontology and saves it to disk. <u><b>NOTE</b></u> that if this function is not
	 * called, then the ontology is not saved.
	 */
	public void close() {
		File file = new File(filename);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
		} catch (FileNotFoundException e1) {
		}
		base.write(out);
	}
}