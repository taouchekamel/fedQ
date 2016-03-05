/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fedQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 *
 * 
 */
public class Main {
	public static void main(String[] args) throws Exception {

		/**
		 * Exécution de l'application par commande (fichier jar) Récupération
		 * des 2 paramêtres : ( param1 : SourceFile, param2 : QueryFile)
		 */
		String SourceFile = "";
		String QueryFile = "";
		boolean useCombinedSources = false;
		if (args.length < 2) {

			SourceFile = "ressources\\endpoints.txt";

			// Param 1 : Query
			QueryFile = "ressources\\requete.txt";

			// System.out.println("Veuillez rajouter les paramêtres :  ( param1 : SourceFile, param2 : QueryFile) .");
			// System.exit(0);
		} else {
			SourceFile = args[0].toString();

			// Param 1 : Query
			QueryFile = args[1].toString();
			useCombinedSources = args[2].toString().equals("1") ? true : false;

		}
		// final String SourceFile
		// ="C://fedQueryJar//fedQuery//ressource//source.txt";

		// final String QueryFile
		// ="C://fedQueryJar//fedQuery//ressource//query.txt";
		// Param 0 : Source

		System.out.println("SourceFile : " + SourceFile);
		System.out.println("QueryFile : " + QueryFile);

		FedQ fedQ = new FedQ(SourceFile, QueryFile);

		// creation et initialisation d un depot local
		Repository repo = fedQ.CreateNativeStore();
		repo.initialize();

		// creation d'une connexion
		RepositoryConnection conLocal = repo.getConnection();

		String[] triplePattern = null;

		//

		try {

			// Récupérer les triple Pattern
			triplePattern = fedQ.getTriplePattern();

			// Faire un Ask de chaque triple Pattern à chaque source pour
			// vérifier l'existance d'un résultat
			HashMap<MapKey, Boolean> askResult = fedQ
					.askSourceForTriplePattern(triplePattern);

			// Sous-requêtes partageant les meme sources
			HashMap<Integer, ArrayList<Integer>> combinedSource = null;
			if (useCombinedSources){
				combinedSource = fedQ
						.getCombinedRequestFromSameSource(askResult);
			}
			

			// Resultat final - Chaque requête d'une source avec Ask True + les
			// requêtes combinées

			fedQ.getResults(conLocal, askResult, combinedSource);

			// afficher le résultat final en soumettant la requete initiale au
			// dépôt local
			fedQ.ReturnFinalResult(conLocal);

			System.out.println("***Fin de traitement");
			System.exit(0);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedQueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (QueryEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
