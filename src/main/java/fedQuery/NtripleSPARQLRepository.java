package fedQuery;

import org.openrdf.repository.sparql.SPARQLRepository;

public class NtripleSPARQLRepository extends SPARQLRepository {
    public NtripleSPARQLRepository(String endpointUrl) {
        super(endpointUrl);
       // this.getHttpClient().setPreferredRDFFormat(RDFFormat.NTRIPLES);
    }
}