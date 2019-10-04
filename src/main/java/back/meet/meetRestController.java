package back.meet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import back.security.config.WebSecurityConfig;
import back.security.model.User;
import back.security.repository.UserRepository;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

//import javassist.bytecode.Descriptor.Iterator;
import java.util.Iterator;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONUtil;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;



@RestController
@RequestMapping("/meet")
public class meetRestController<E> {
	private final Log logger = LogFactory.getLog(this.getClass());
    @Autowired
    private meetRepository meetRepository;
    

    
    
    @RequestMapping(method = RequestMethod.GET)
    public List<meet> findAll() throws IOException {
    	

    	
    	
        return meetRepository.findAll();
        
        
    }
    
	@RequestMapping(method = RequestMethod.GET, value = "/{meetId}")
    public meet findOne(@PathVariable Long meetId) {
        return meetRepository.findOne(meetId);
    }
    
	
	private PDDocument getPdfDocumentFromJson( String json_string ) throws ParseException, IOException
	{
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(json_string);
		logger.info("Parametro del body: \t"+json.get("pdf_base_64") );
		
		String pfg_string = (String) json.get("pdf_base_64");	
		logger.info("Parametro codificado a String: \t"+pfg_string);	  
		
		byte[] decoder = Base64.getDecoder().decode(pfg_string);
		return PDDocument.load(decoder);
		
	}
	
	
	@RequestMapping(method = RequestMethod.POST, value = "/tag_analyzer", produces = "application/json")
    public AnalizedPdfResponse analyze(@RequestBody String json_string) throws ParseException, IOException {
		String[] tags = { "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT" , "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB" };
		String[] tags_description = { "Coordinating conjunction", "Cardinal number", "Determiner", "Existential there", "Foreign word", "Preposition or subordinating conjunction", "Adjective", "Adjective, comparative", "Adjective, superlative", "List item marker", "Modal", "Noun, singular or mass", "Noun, plural", "Proper noun, singular", "Proper noun, plural", "Predeterminer" , "Possessive ending", "Personal pronoun", "Possessive pronoun", "Adverb", "Adverb, comparative", "Adverb, superlative", "Particle", "Symbol", "to", "Interjection", "Verb, base form", "Verb, past tense", "Verb, gerund or present participle", "Verb, past participle", "Verb, non-3rd person singular present", "Verb, 3rd person singular present", "Wh-determiner", "Wh-pronoun", "Possessive wh-pronoun", "Wh-adverb" };
		
		AnalizedPdfResponse respuesta = new AnalizedPdfResponse();
		respuesta.setAbbreviation_tags(tags);
		respuesta.setDescription_tags(tags_description);
		
		//Conversion de JSON base64 -> PDF -> String Arraylist
		/////////////////////////////////////		
		
		PDDocument document = this.getPdfDocumentFromJson(json_string) ;		
		ArrayList< String > pdf_arraylist = this.convertPdfToStringArray(document);
		
		
		//Analisis de texto
		//////////////////////////////////////////////////////////////////////////////////////		
		//Iteracion para analizar los tag de cada string de este arreglo		
		Iterator pdf_arraylist_iterator = (Iterator) pdf_arraylist.iterator();		

		int[] tagVals = new int[36] ;
		Arrays.fill(tagVals, 0);
		
		while( pdf_arraylist_iterator.hasNext() )
		{
			int [] newVals = this.analyzeString( (String) pdf_arraylist_iterator.next() );
			
			for( int i = 0; i < tagVals.length; i ++)
			{
				tagVals[i] = tagVals[i] + newVals[i];	
			}			
				
		}
		respuesta.setValues_tags(tagVals);

		
		return respuesta;		
		
    }
	
	
	private int[] analyzeString( String string )
	{
		 
		MaxentTagger tagger = new MaxentTagger("wsj-0-18-left3words-distsim.tagger");
		List<HasWord> sentences = SentenceUtils.toWordList(string.split(" "));
		List<TaggedWord> tSentence = tagger.tagSentence(sentences);		
		logger.info("Sentencia a analizar: \t"+tSentence);	
		
		/*
		 *
		 * 
		 * 1. 	CC 	Coordinating conjunction
		2. 	CD 	Cardinal number
		3. 	DT 	Determiner
		4. 	EX 	Existential there
		5. 	FW 	Foreign word
		6. 	IN 	Preposition or subordinating conjunction
		7. 	JJ 	Adjective
		8. 	JJR 	Adjective, comparative
		9. 	JJS 	Adjective, superlative
		10. 	LS 	List item marker
		11. 	MD 	Modal
		12. 	NN 	Noun, singular or mass
		13. 	NNS 	Noun, plural
		14. 	NNP 	Proper noun, singular
		15. 	NNPS 	Proper noun, plural
		16. 	PDT 	Predeterminer
		17. 	POS 	Possessive ending
		18. 	PRP 	Personal pronoun
		19. 	PRP$ 	Possessive pronoun
		20. 	RB 	Adverb
		21. 	RBR 	Adverb, comparative
		22. 	RBS 	Adverb, superlative
		23. 	RP 	Particle
		24. 	SYM 	Symbol
		25. 	TO 	to
		26. 	UH 	Interjection
		27. 	VB 	Verb, base form
		28. 	VBD 	Verb, past tense
		29. 	VBG 	Verb, gerund or present participle
		30. 	VBN 	Verb, past participle
		31. 	VBP 	Verb, non-3rd person singular present
		32. 	VBZ 	Verb, 3rd person singular present
		33. 	WDT 	Wh-determiner
		34. 	WP 	Wh-pronoun
		35. 	WP$ 	Possessive wh-pronoun
		36. 	WRB 	Wh-adverb
		 *
		 * */
		int[] counter = new int[36] ;
		Arrays.fill(counter, 0);
		String[] tags = { "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT" , "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB" };
		String[] tags_description = { "Coordinating conjunction", "Cardinal number", "Determiner", "Existential there", "Foreign word", "Preposition or subordinating conjunction", "Adjective", "Adjective, comparative", "Adjective, superlative", "List item marker", "Modal", "Noun, singular or mass", "Noun, plural", "Proper noun, singular", "Proper noun, plural", "Predeterminer" , "Possessive ending", "Personal pronoun", "Possessive pronoun", "Adverb", "Adverb, comparative", "Adverb, superlative", "Particle", "Symbol", "to", "Interjection", "Verb, base form", "Verb, past tense", "Verb, gerund or present participle", "Verb, past participle", "Verb, non-3rd person singular present", "Verb, 3rd person singular present", "Wh-determiner", "Wh-pronoun", "Possessive wh-pronoun", "Wh-adverb" };
		
		
		for (TaggedWord tw : tSentence) 
		{
			
			for ( int i = 0 ; i < tags.length ; i++)
			{
				if( tw.tag().startsWith( tags[i] ) )
				{
					counter[i] = counter[i] + 1;
				}
			}	

			
		}
		
		return counter;	
		
		
	}
	

	
	
	
	private ArrayList< String > convertPdfToStringArray( PDDocument pdf_file) throws IOException{		
		
		ArrayList<String> pdf_arraylist = new ArrayList<String>();
		
    	if (!pdf_file.isEncrypted()) {
    	    PDFTextStripper stripper = new PDFTextStripper();
    	    String text = stripper.getText(pdf_file);
    	    pdf_arraylist.add(text);
    	    logger.info("Text in document throw PDF Reader: \t" + text);
    	}
    	
    	
    	
    	pdf_file.close();
		
		
		
		return pdf_arraylist;
		
	}
	
	
	
	
	

	
	
	
	
	
	
	
	

	@RequestMapping(method = RequestMethod.PUT)
    public meet update(@RequestBody meet meet) {
        return meetRepository.save(meet);
    }
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{meetId}")
    public void delete(@PathVariable Long meetId) {
        meetRepository.delete(meetId);
    }
	
}

