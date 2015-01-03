package dmeeuwis.nakama.kanjidraw;

import java.util.ArrayList;
import java.util.List;

public class Criticism {
	public final List<String> critiques;
	public boolean pass;
	
	public Criticism(){
		this.critiques = new ArrayList<String>();
		this.pass = true;
	}
	
	public void add(String critique){
		this.critiques.add(critique);
		this.pass = false;
	}
}
