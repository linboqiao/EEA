package limo.exrel.modules.mallet.MalletMaxEnt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.regex.Pattern;

import cc.mallet.extract.StringSpan;
import cc.mallet.extract.StringTokenization;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import cc.mallet.util.CharSequenceLexer;

public class CharSequence2TokenSequenceSimple  extends Pipe implements Serializable {
	
	CharSequenceLexer lexer;
	
	public CharSequence2TokenSequenceSimple (CharSequenceLexer lexer)
	{
		this.lexer = lexer;
	}

	public CharSequence2TokenSequenceSimple (String regex)
	{
		this.lexer = new CharSequenceLexer (regex);
	}

	public CharSequence2TokenSequenceSimple (Pattern regex)
	{
		this.lexer = new CharSequenceLexer (regex);
	}
	
	public Instance pipe (Instance carrier)
	{
		CharSequence string = (CharSequence) carrier.getData();
		lexer.setCharSequence (string);
		TokenSequence ts = new TokenSequence();
		while (lexer.hasNext()) {
			ts.add ((String) lexer.next());
		}
		carrier.setData(ts);
		return carrier;
	}
	
	// Serialization 
	
		private static final long serialVersionUID = 1;
		private static final int CURRENT_SERIAL_VERSION = 0;
		
		private void writeObject (ObjectOutputStream out) throws IOException {
			out.writeInt(CURRENT_SERIAL_VERSION);
			out.writeObject(lexer);
		}
		
		private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
			int version = in.readInt ();
			lexer = (CharSequenceLexer) in.readObject();
		}

}
