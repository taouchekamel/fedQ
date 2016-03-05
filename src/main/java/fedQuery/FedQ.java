/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fedQuery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.runtime.ANTLRFileStream;
import org.openrdf.model.Statement;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.openrdf.sail.nativerdf.NativeStore;

/**
 *
 * 
 */
public class FedQ {

	private String QUERY_FILE = "";
	private String SOURCE_FILE = "";

	public FedQ(String SOURCE_FILE, String QUERY_FILE) {

		this.setQUERY_FILE(QUERY_FILE);
		this.setSOURCE_FILE(SOURCE_FILE);

	}

	// Creation d'un depot local
	public Repository CreateNativeStore() throws RepositoryException {
		File dataDir = new File("nativestore");
		String indexes = "spoc,posc,cosp";
		Repository repositoryLocal = new SailRepository(new NativeStore(
				dataDir, indexes));
		repositoryLocal.initialize();
		return repositoryLocal;
	}

	// afficher le contenu d'un repo
	public void ReturnFinalResult(RepositoryConnection connectionLocal)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException, IOException {

		String requete = requetePrincipale();
		// System.out.println("requetePrincipale:" +requete);
		int nbrResult = 0;

		GraphQueryResult graphResult;
		graphResult = connectionLocal.prepareGraphQuery(QueryLanguage.SPARQL,
				requete).evaluate();
		while (graphResult.hasNext()) {
			nbrResult++;
			Statement anRDFStatement = graphResult.next();
			// ici on se contente d'afficher le Statement

			System.out.println(anRDFStatement);
			System.out.println("------------------");
		}
		System.out.println(" résultats trouvés : " + nbrResult);
	}

	// interoger les end points par ASK
	public Boolean askSource(String request, RepositoryConnection connection)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		String requteASK = "ASK  {" + request + "}";
		System.out.println(requteASK);

		// on initialise la query.
		BooleanQuery booleanQuery = connection.prepareBooleanQuery(
				QueryLanguage.SPARQL, requteASK);

		// on l'exécute
		Boolean result = booleanQuery.evaluate();

		return result;

	}

	// recuperer les triples patternes
	public String[] getTriplePattern() throws IOException {
		ANTLRFileStream inputQuery = new ANTLRFileStream(QUERY_FILE);
		String req = inputQuery.toString();
		return req.split(System.getProperty("line.separator"));
	}

	// Recuperer les endPoints
	public String[] getEndPointsList() throws IOException {
		ANTLRFileStream inputSource = new ANTLRFileStream(SOURCE_FILE);
		String sources = inputSource.toString();
		String[] linesSources = sources.split(System
				.getProperty("line.separator"));
		System.out.println("nbr de sources:" + linesSources.length);

		return linesSources;
	}

	// recuperer les resultats comme graphes et les ajouter au depot local
	public void FromSourceToRepo(RepositoryConnection conRepo, String request,
			RepositoryConnection connection) throws Exception,
			MalformedQueryException {
		int nbrResu = 1;

		// /////////////////////
		try {
			String constructQuery = "CONSTRUCT  { " + request + "} where  { "
					+ request + "} LIMIT 900";

			GraphQueryResult graphResult;
			graphResult = connection.prepareGraphQuery(QueryLanguage.SPARQL,
					constructQuery).evaluate();
			System.out.println(constructQuery);
			try {
				while (graphResult.hasNext()) {

					// ajouter le graphe au depot local
					// //remarque: c'est plus simple d'utiliser une variable
					Statement res = graphResult.next();

					// con.add(graphResult.next().getSubject(),
					// graphResult.next().getPredicate(),
					// graphResult.next().getObject());
					conRepo.add(res.getSubject(), res.getPredicate(),
							res.getObject());

					System.out.println((nbrResu++) + "graphe   :" + res);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				graphResult.close();
			}
		} finally {
			connection.close();
		}

		// ///////////

	}

	// interoger les endpoints pour chaque TP
	public HashMap<MapKey, Boolean> askSourceForTriplePattern(
			String[] triplePattern) throws IOException, RepositoryException,
			MalformedQueryException, QueryEvaluationException {

		String[] linesSources = getEndPointsList();
		HashMap<MapKey, Boolean> askResult = new HashMap<MapKey, Boolean>();
		RepositoryConnection con;
		// Parcourir les endPoints pour faire une requete Ask
		for (int i = 0; i < linesSources.length; i++) {
			// Demande de connection à la source

			Repository r = new SPARQLRepository(linesSources[i]);
			r.initialize();
			con = r.getConnection();
			for (int j = 0; j < triplePattern.length; j++) {
				// Récuperer le résultat
				askResult.put(new MapKey(i, j),
						askSource(triplePattern[j], con));
			}
			con.close();
		}
		return askResult;
	}

	// recuperer les resultats et les ajouter dans le depot local
	public void getResults(RepositoryConnection conLocal,
			HashMap<MapKey, Boolean> askResult) throws Exception {
		// recuperer les triples patterns et les endpoints
		String[] linesSources = getEndPointsList();
		String[] triplePattern = getTriplePattern();

		RepositoryConnection connection;

		// Parcourir les endPoints pour faire un Select
		for (int i = 0; i < linesSources.length; i++) {
			// Demande de connection Ã  la source
			Repository r = new SPARQLRepository(linesSources[i]);
			r.initialize();
			connection = r.getConnection();

			for (int j = 0; j < triplePattern.length; j++) {
				// Si le Ask est true, on exucute pour récupérer le résultat
				if (askResult.get(new MapKey(i, j)) != null
						&& askResult.get(new MapKey(i, j))) {
					// nombreDeResultat = nombreDeResultat +
					System.out.println("triplePattern:"
							+ triplePattern[j].toString());
					// charger les fichier dans un repo && Interroger le depot
					// local
					FromSourceToRepo(conLocal, triplePattern[j], connection);

				}
			}

			connection.close();
		}

	}

	// /construire la requete initiale à partir du fichier requete
	public String requetePrincipale() throws IOException {
		String[] triplePattern = null;
		String q = "";
		triplePattern = getTriplePattern();
		for (int i = 0; i < triplePattern.length; i++) {

			q += triplePattern[i] + ".";

		}
		return q = "CONSTRUCT  { " + q + "}  where  { " + q + "}";// LIMIT 5";

	}

	public HashMap<Integer, ArrayList<Integer>> getCombinedRequestFromSameSource(
			HashMap<MapKey, Boolean> askResult) throws IOException {

		// Objet qui va contenir la liste des TP qui peuvent etres résolus par
		// le meme endPoint
		HashMap<Integer, ArrayList<Integer>> combinedQuery = new HashMap<Integer, ArrayList<Integer>>();

		// Liste des endPoint
		String[] endPoints = getEndPointsList();

		// On itère sur les sources
		for (int i = 0; i < endPoints.length; i++) {

			// Liste des requêtes qui ont la meme source
			ArrayList<Integer> listOfRequest = new ArrayList<Integer>();

			for (int j = 0; j < askResult.size(); j++) {

				// On vérifie si la requête existe dans la source actuelle
				if (askResult.get(new MapKey(i, j)) != null
						&& askResult.get(new MapKey(i, j))) {

					listOfRequest.add(j);

				}

			}
			// //combiner les sous requete------------------------------
			combinedQuery.put(i, listOfRequest);

			listOfRequest = null;

		}

		return combinedQuery;

	}

	public void getResults(RepositoryConnection conLocal,
			HashMap<MapKey, Boolean> askResult,
			HashMap<Integer, ArrayList<Integer>> combinedSource)
			throws Exception {

		String[] linesSources = getEndPointsList();
		String[] triplePattern = getTriplePattern();

		// /FileOutputStream out = new
		// FileOutputStream("ressource//result.srx");
		RepositoryConnection connection;
		// int nombreDeResultat = 0;
		// Parcourir les endPoints pour faire un Select
		System.out.println("---------> Requêtes single");
		for (int i = 0; i < linesSources.length; i++) {
			// Demande de connection à la source
			connection = getConnectionFromEndPoint(linesSources[i]);
			for (int j = 0; j < triplePattern.length; j++) {
				// Si le Ask est true, on exécute pour récupérer le résultat
				if (askResult.get(new MapKey(i, j)) != null
						&& askResult.get(new MapKey(i, j))) {
					// nombreDeResultat = nombreDeResultat +
					// selectFromSource(triplePattern[j], connection);

					FromSourceToRepo(conLocal, triplePattern[j], connection);
				}
			}

			if (combinedSource != null) {
				System.out.println("---------> Requêtes combinées");
				// Ajouter les requêtes combinées
				ArrayList<Integer> combinedRequests = combinedSource.get(i);
				// nombreDeResultat = nombreDeResultat+
				selectFromMultipleSource(conLocal, linesSources, triplePattern,
						combinedRequests, connection);
			}

			connection.close();
		}

	}

	private void selectFromMultipleSource(RepositoryConnection conLocal,
			String[] linesSources, String[] triplePattern,
			ArrayList<Integer> combinedRequests, RepositoryConnection connection)
			throws Exception {

		// Construire la requête avec WHERE des '.' entre les requêtes
		String requteOrigine = "";
		for (int i = 0; i < combinedRequests.size(); i++) {

			requteOrigine = requteOrigine
					+ triplePattern[combinedRequests.get(i)] + ". ";
		}

		FromSourceToRepo(conLocal, requteOrigine, connection);
	}

	private RepositoryConnection getConnectionFromEndPoint(String endPoint)
			throws RepositoryException {
		// String typeSource = endPointWithType.substring(1, 2);
		// String endPoint = endPointWithType;
		// Object repositoryObj;

		Repository repository = new SPARQLRepository(endPoint);

		// Repository repository = (Repository) repositoryObj;
		repository.initialize();
		// on ouvre une connexion au repository
		// comme en JDBC, c'est à travers cette connexion que sont envoyées
		// toute les requêtes
		return repository.getConnection();
	}

	public String getQUERY_FILE() {
		return QUERY_FILE;
	}

	public void setQUERY_FILE(String qUERY_FILE) {
		QUERY_FILE = qUERY_FILE;
	}

	public String getSOURCE_FILE() {
		return SOURCE_FILE;
	}

	public void setSOURCE_FILE(String sOURCE_FILE) {
		SOURCE_FILE = sOURCE_FILE;
	}

}
