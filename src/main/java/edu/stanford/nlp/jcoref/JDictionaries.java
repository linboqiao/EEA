package edu.stanford.nlp.jcoref;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.jcoref.docclustering.TfIdf;

public class JDictionaries extends Dictionaries {
  public static final double THESAURUS_THRESHOLD = 0.7;
  public static final double THESAURUS_SCORE_THRESHOLD = 0.7;
  public final Set<String> lightVerb = new HashSet<String>(Arrays.asList("have", "make", "get", "take", "receive"));
  public final Set<String> reportNoun = new HashSet<String>(Arrays.asList(
      "ABCs", "acclamation", "account", "accusation", "acknowledgment", "address", "addressing",
      "admission", "advertisement", "advice", "advisory", "affidavit", "affirmation", "alert",
      "allegation", "analysis", "anecdote", "annotation", "announcement", "answer", "antiphon",
      "apology", "applause", "appreciation", "argument", "arraignment", "article", "articulation",
      "aside", "assertion", "asseveration", "assurance", "attestation", "attitude", "audience",
      "averment", "avouchment", "avowal", "axiom", "backcap", "band-aid", "basic", "belief", "bestowal",
      "bill", "blame", "blow-by-blow", "bomb", "book", "bow", "break", "breakdown", "brief", "briefing",
      "broadcast", "broadcasting", "bulletin", "buzz", "cable", "calendar", "call", "canard", "canon",
      "card", "cause", "censure", "certification", "characterization", "charge", "chat", "chatter",
      "chitchat", "chronicle", "chronology", "citation", "claim", "clarification", "close", "cognizance",
      "comeback", "comment", "commentary", "communication", "communique", "composition", "concept",
      "concession", "conference", "confession", "confirmation", "conjecture", "connotation", "construal",
      "construction", "consultation", "contention", "contract", "convention", "conversation", "converse",
      "conviction", "cooler", "copy", "counterclaim", "crack", "credenda", "credit", "creed", "critique",
      "cry", "data", "declaration", "defense", "definition", "delineation", "delivery", "demonstration",
      "denial", "denotation", "depiction", "deposition", "description", "detail", "details", "detention",
      "dialogue", "diction", "dictum", "digest", "directive", "dirt", "disclosure", "discourse", "discovery",
      "discussion", "dispatch", "display", "disquisition", "dissemination", "dissertation", "divulgence",
      "dogma", "earful", "echo", "edict", "editorial", "ejaculation", "elucidation", "emphasis", "enlightenment",
      "enucleation", "enunciation", "essay", "evidence", "examination", "example", "excerpt", "exclamation",
      "excuse", "execution", "exegesis", "explanation", "explication", "exposing", "exposition", "expounding",
      "expression", "eye-opener", "feedback", "fiction", "findings", "fingerprint", "flash", "formulation",
      "fundamental", "gift", "gloss", "goods", "gospel", "gossip", "grapevine", "gratitude", "greeting",
      "guarantee", "guff", "hail", "hailing", "handout", "hash", "headlines", "hearing", "hearsay", "history",
      "ideas", "idiom", "illustration", "impeachment", "implantation", "implication", "imputation",
      "incrimination", "inculcation", "indication", "indoctrination", "inference", "info", "information",
      "innuendo", "insinuation", "insistence", "instruction", "intelligence", "interpretation", "interview",
      "intimation", "intonation", "issue", "item", "itemization", "justification", "key", "knowledge",
      "language", "leak", "letter", "line", "lip", "list", "locution", "lowdown", "make", "manifesto",
      "meaning", "meeting", "mention", "message", "missive", "mitigation", "monograph", "motive", "murmur",
      "narration", "narrative", "news", "nod", "note", "notice", "notification", "oath", "observation",
      "okay", "opinion", "oral", "outline", "paper", "parley", "particularization", "phrase", "phraseology",
      "phrasing", "picture", "piece", "pipeline", "pitch", "plea", "plot", "poop", "portraiture", "portrayal",
      "position", "potboiler", "prating", "precept", "prediction", "presentation", "presentment", "principle",
      "proclamation", "profession", "program", "promulgation", "pronouncement", "pronunciation", "propaganda",
      "prophecy", "proposal", "proposition", "prosecution", "protestation", "publication", "publicity",
      "publishing", "quotation", "ratification", "reaction", "reason", "rebuttal", "receipt", "recital",
      "recitation", "recognition", "record", "recount", "recountal", "refutation", "regulation", "rehearsal",
      "rejoinder", "relation", "release", "remark", "rendition", "repartee", "reply", "report", "reporting",
      "representation", "resolution", "response", "result", "retort", "return", "revelation", "review",
      "riposte", "rule", "rumble", "rumor", "rundown", "salutation", "salute", "saying", "scandal", "scoop",
      "scuttlebutt", "sense", "showing", "sign", "signature", "significance", "sketch", "skinny", "solution",
      "speaking", "specification", "speech", "spiel", "statement", "story", "study", "style", "suggestion",
      "summarization", "summary", "summons", "tale", "talk", "talking", "tattle", "teaching", "telecast",
      "telegram", "telling", "tenet", "term", "testimonial", "testimony", "text", "theme", "thesis", "tidings",
      "topper", "tract", "tractate", "tradition", "translation", "treatise", "utterance", "vent", "ventilation",
      "verbalization", "version", "vignette", "vindication", "vocalization", "voice", "voicing", "warning",
      "warrant", "whispering", "wire", "wisecrack", "word", "work", "writ", "write-up", "writeup", "writing", "yarn"
  ));
  public final Set<String> nonEventVerb = new HashSet<String>(Arrays.asList(
      "be", "have", "seem"));
  public final TfIdf tfIdf = new TfIdf();

  public final Map<String, Set<String>> thesaurusVerb = new HashMap<String, Set<String>>();
  public final Map<String, Set<String>> thesaurusNoun = new HashMap<String, Set<String>>();
  public final Map<String, Set<String>> thesaurusAdj = new HashMap<String, Set<String>>();
 

  /** load Dekang Lin's thesaurus (Proximity based). */
  private void loadThesaurus(String file, Map<String, Set<String>> thesaurus) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(file)));
      int count = -1;
      String word = "";
      while(reader.ready()) {
        String[] split = reader.readLine().toLowerCase().split("\t");
        if(split[0].startsWith("(")) {
          word = split[0].split("\\(")[1].trim();
          if(word.startsWith("C_")) word = word.substring(2);
          thesaurus.put(word, new HashSet<String>());
          count = 0;
        } else if(split[0].equals("))")) {
          continue;
        } else {
          if(count++ < THESAURUS_THRESHOLD) {
            double score = Double.parseDouble(split[1]);
            if(split[0].startsWith("C_")) split[0] = split[0].substring(2);
            if(score > THESAURUS_SCORE_THRESHOLD) thesaurus.get(word).add(split[0]);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeIgnoringExceptions(reader);
    }
  }
 
  public JDictionaries(Properties props) {
    super(props);
    String thesaurusVerbFile = props.getProperty(JConstants.THESAURUS_VERB_PROP, "/scr/heeyoung/corpus/DekangLinSyntaxBasedThesaurus/simV.lsp");
    String thesaurusNounFile = props.getProperty(JConstants.THESAURUS_NOUN_PROP, "/scr/heeyoung/corpus/DekangLinSyntaxBasedThesaurus/simN.lsp");
    String thesaurusAdjFile = props.getProperty(JConstants.THESAURUS_ADJ_PROP, "/scr/heeyoung/corpus/DekangLinSyntaxBasedThesaurus/simA.lsp");
    loadThesaurus(thesaurusVerbFile, thesaurusVerb);
    loadThesaurus(thesaurusNounFile, thesaurusNoun);
    loadThesaurus(thesaurusAdjFile, thesaurusAdj);
  }
}
