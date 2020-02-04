import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
//import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
//import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
//import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author tungh
 *		uses MRV heuristic (i.e. items with fewer positive compatibilities will be processed first) -> most likely to be bagged first.
 *		uses LCV heuristic (i.e. binary search for compatibilities) -> reduces search space and improves search speed significantly.
 *		uses arc consistency checking in order to improve search speed.
 *		implements a local search method (i.e. alternative universe).
 */

public class P1_Solver_v4_Arc_ {
	
	public static int numbags=-1;					// tracks total number of bags available.
	public static int sizebags=-1;						// records maximum allowable weight per bag.
	
	// TH: hashMap of item: Integer, String.
	public static Map< Integer, String> hm_reverse = new HashMap< Integer, String>();
	
	// TH: hashMap of item: id, weight.
	public static Map< String,Integer> hm = new HashMap< String,Integer>();
	public static Map< String,Integer> hm_weight = new HashMap< String,Integer>();
	
	// TH: hashMap of compatibility, String.
	public static Map< String,Vector<String>> hm_plus = new HashMap< String,Vector<String>>();
	public static Map< String,Vector<String>> hm_minus = new HashMap< String,Vector<String>>();
	
	// TH: hashMap of compatibility, Integer.
	public static Map< Integer,Vector<Integer>> hm_plus_int_ = new HashMap< Integer,Vector<Integer>>();
	public static Map< Integer,Vector<Integer>> hm_minus_int_ = new HashMap< Integer,Vector<Integer>>();
	
	// TH: tracks actual number of positive constraints, Integer.
	public static Map< Integer,Integer> hm_plus_counter_ = new HashMap< Integer,Integer>();
	
	// TH: Vector of all String items.
	public static Vector<String> hm_all_ = new Vector<String>();
	
	// TH: initially tracks all items in the problem.
	public static Vector<Integer> original_ = new Vector<Integer>();
	
	// TH: displays very 1st solution found and exits.
	private static boolean doItOnce = true;
	
	/**
	 * TODO: 
	 * 		computes standard deviations and project specifications ...
	 * 		considers IMPORTANT sections for either key information or expansions into more dimensions.
	 * 		avoids using reference as a seed in Vector<Vector<Integer>> constructor.
	 * 		finds the best of the best solution when free time.
	 * 		avoids using addAll when dealing with Vector<Vector<Integer>> to Vector<Vector<Integer>>.
	 * 		builds tree (AVL) for optimization.
	 * 		avoids JAVA API HashSet<Vector<Vector<Integer>>>
	 * @param args
	 */
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		// INFO: https://cs.stackexchange.com/questions/47870/what-is-least-constraining-value
		// MRV versus LCV: https://courses.cs.washington.edu/courses/cse473/16au/slides-16au/08-CSP3.pdf
		// MRV is fail-fast where LCV is succeed fast.
		
//		SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
//		Date date = new Date(System.currentTimeMillis());
//		System.out.println(formatter.format(date));
		
		String  mode = "-depth";
		
		if(args.length==0 || args.length>2) {
			System.out.println("failure"); System.exit(1);
		}// if len==0.
		if(args.length==1) {
			;
		}// if len==1.
		if(args.length==2) {
			mode = args[1];
		}// if len==2.
		
		// TH: unique sets to be distributed out to all available buckets.
		Vector< Vector<Integer>> unique_sets = new Vector< Vector<Integer>>();
		
		// https://www.geeksforgeeks.org/different-ways-reading-text-file-java/
		File file = new File(args[0]);
//		File file = new File("C:\\Users\\tungh\\eclipse-workspace\\CS457-P1\\P1_\\src\\test_0_.txt");
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String st; StringTokenizer tokens; int id=-3;
			
			try {
				labelA:
				while ((st = br.readLine()) != null) {
//					System.out.println(st); 
					if (numbags==-1) {
						// TH: parses integer for number of bags with try/catch.
						try {
							numbags = Integer.parseInt(st);
							if (numbags<0) {
//								throw new IllegalArgumentException("Invalid Input: Number of Bags !!");
								System.out.println("failure"); System.exit(1);
							}//if < 0.
//							System.out.println(numbags); 
							id++; continue labelA;
						}//try.
						catch (NumberFormatException e){
//							System.out.println("Invalid Input: Number of Bags !!");
//							e.printStackTrace();
							System.out.println("failure"); System.exit(1);
						}// catch NumberFormatException.
					}//if numbags.
					if(sizebags==-1) {
						// TH: parses integer for size of bags with try/catch.
						try {
							sizebags = Integer.parseInt(st);
							if (sizebags<0) {
//								throw new IllegalArgumentException("Invalid Input: Size of Bags !!");
								System.out.println("failure"); System.exit(1);
							}//if < 0.
//							System.out.println(sizebags); 
							id++; continue labelA;
						}//try.
						catch (NumberFormatException e){
//							System.out.println("Invalid Input: Size of Bags !!");
//							e.printStackTrace();
							System.out.println("failure"); System.exit(1);
						}// catch NumberFormatException.
					}//if sizebags.
					
					// TH: parses individual remaining lines.
					tokens = new StringTokenizer(st);
					
					// https://www.geeksforgeeks.org/map-interface-java-examples/
					if(tokens.countTokens()>2) {
						
						// TH: parses item, weight, and sign.
						String item="dummy", weight="-1", sign="?";
						if(tokens.hasMoreTokens()) { item = tokens.nextToken(); }
						if(tokens.hasMoreTokens()) { 
							weight = tokens.nextToken();
							if(Integer.parseInt(weight)<0) {
//								throw new IllegalArgumentException("Invalid Input: Item Weight !!");
								System.out.println("failure"); System.exit(1);
							}//if weight < 0.
							if(Integer.parseInt(weight)>sizebags) {
								System.out.println("failure"); System.exit(1);
							}//if weight>sizebags.
						}//if hasMoreTokens for weight.
						if(tokens.hasMoreTokens()) {
							if(((String)sign).compareTo("?")==0) {
								sign = tokens.nextToken();
//								System.out.println("Sign: "+ sign); 
								if(((String)sign).length()!=1) {
//									throw new IllegalArgumentException("Invalid Input: Item Signage 0 !!");
									System.out.println("failure"); System.exit(1);
								}// if len!=1.
								if(((String)sign).compareTo("+")!=0 && ((String)sign).compareTo("-")!=0) {
//									throw new IllegalArgumentException("Invalid Input: Item Signage 1 !!");
									System.out.println("failure"); System.exit(1);
								}// if NOT + and NOT - signs.
							}
						}//if hasMoreTokens for signage.
						
						// TH: checks empty compatibility list.
						if(((String)sign).compareTo("+")==0 || ((String)sign).compareTo("-")==0) {
							if(!tokens.hasMoreTokens()) {
//								throw new IllegalArgumentException("Invalid Input: Not Enough Item Information !!");
								System.out.println("failure"); System.exit(1);
							}
						}//if + or -, but NO compatible items.
						
						// TH: updates hashMap for item: id, weight.
						id++; 
						hm.put(item, (Integer) id);
//						if(Integer.parseInt(weight)<0) throw new IllegalArgumentException("Invalid Input: Item Weight !!");
						hm_weight.put(item, Integer.parseInt(weight));
						
						if(((String)sign).compareTo("+")==0) {
							// TH: updates positive dependency.
							Vector<String> items_plus = new Vector<String>();
							while(tokens.hasMoreTokens()) { 
//								String temp_=(String)tokens.nextToken();
//								System.out.println(temp_);
								items_plus.add((String)tokens.nextToken());
							}//while hasMoreTokens AND list of + items.
							hm_plus.put(item, items_plus);
							hm_minus.put(item, new Vector<String>());
						}//if +.
						else if(((String)sign).compareTo("-")==0) {
							// TH: updates negative dependency.
							Vector<String> items_minus = new Vector<String>();
							while(tokens.hasMoreTokens()) { 
//								String temp_=(String)tokens.nextToken();
//								System.out.println(temp_);
								items_minus.add((String)tokens.nextToken());
							}//while hasMoreTokens AND list of - items.
							hm_minus.put(item, items_minus);
							hm_plus.put(item, new Vector<String>());
						}//else if -.
						else {
//							System.out.println("WARNING: Should NOT be here !!");
							System.out.println("failure"); System.exit(1);
						}
						continue labelA;
					}// if >2.
					else if(tokens.countTokens()==2) {
						
						/*
						 * TH: no compatibility available, simply stores id and weight.
						 */
//						System.out.println("No Compatibility Provided.");
						
						String item = tokens.nextToken();
						String weight = tokens.nextToken();
						if(Integer.parseInt(weight)<0) {
//							throw new IllegalArgumentException("Invalid Input: Item Weight !!");
							System.out.println("failure"); System.exit(1);
						}//if weight<0.
						// TH: updates hashMap for item: id, weight.
						id++; 
						hm.put(item, (Integer) id);
						hm_weight.put(item, Integer.parseInt(weight));
						
						continue labelA;
					}// if==2.
					else {
//						throw new IllegalArgumentException("Invalid Input: Not Enough Item Information !!");
						System.out.println("failure"); System.exit(1);
					}// else<2.
					
				}//while readLine.
				
			}// try readLine.
			catch (IOException e) {
//				System.out.println("readLine Failed !!");
//				e.printStackTrace();
				System.out.println("failure"); System.exit(1);
			}//catch IOException.
			
		} //try BufferedReader.
		catch (FileNotFoundException e) {
//			System.out.println("BufferedReader or FileReader Failed !!");
//			e.printStackTrace();
			System.out.println("failure"); System.exit(1);
		}//catch FileNotFoundException.
		
//		System.out.println("Mapping String To Id:");
//		Set< Map.Entry< String,Integer> > st_hm = hm.entrySet();
//		for (Map.Entry< String,Integer> map_entry:st_hm) {
//			System.out.print(map_entry.getKey()+": "); 
//			System.out.println(map_entry.getValue() + " (id)"); 
//		}
		
//		System.out.println("Mapping String To Weight:");
//		Set< Map.Entry< String,Integer> > st_hm_weight = hm_weight.entrySet();
//		for (Map.Entry< String,Integer> map_entry:st_hm_weight) {
//			System.out.print(map_entry.getKey()+": "); 
//			System.out.println(map_entry.getValue() + " (weight)"); 
//		}
		
		{
			// TH: builds reverse hash Map.
			Set< Map.Entry< String,Integer> > st_hm_2nd = hm.entrySet();
			for (Map.Entry< String,Integer> map_entry:st_hm_2nd) {
				hm_reverse.put(map_entry.getValue(), map_entry.getKey());
			}
		}
		
		{
			int hm_size = hm.size();
			// TH: initially tracks all items in the problem.
			for(int i=0; i< hm_size; i++) {
				original_.add((Integer)i);
			}//for i.
//			System.out.println("original_: " + original_);
		}
		
		// TH: makes sure that hm_plus is populated properly at this point.
		{
			int hm_size = hm.size();
			labelI1:
			for(int i1=0; i1< hm_size; i1++) {
				String host_ = hm_reverse.get(i1);
				if(hm_plus.get(host_)!=null) {
					if(hm_plus.get(host_).size()==0 && hm_minus.get(host_).size()!=0) continue labelI1;
					if(hm_plus.get(host_).size()!=0) continue labelI1;
				}
				Vector<String> items_plus = new Vector<String>();
				for(int i=0; i< hm_size; i++) {
					items_plus.add(hm_reverse.get(i));
				}//for i.
				items_plus.remove(host_);
//				System.out.println("host_: " + host_);
//				System.out.println("items_plus: " + items_plus);
				hm_plus.put(host_, items_plus);
				hm_minus.put(host_, new Vector<String>());
			}//for i.
		}
		
//		System.out.println("Mapping Id To String:");
//		Set< Map.Entry< Integer,String> > st_hm_reverse = hm_reverse.entrySet();
//		for (Map.Entry< Integer,String> map_entry:st_hm_reverse) {
//			System.out.print(map_entry.getKey()+": "); 
//			System.out.println(map_entry.getValue() + " (id)"); 
//		}
		
		{
			// TH: builds a hashMap of all String items.
			Set< Map.Entry< String,Integer> > st_hm_3rd = hm.entrySet();
			for (Map.Entry< String,Integer> map_entry:st_hm_3rd) hm_all_.add(map_entry.getKey());
		}
		
//		// TH: displays hm_plus.
//		Set<Entry<String, Vector<String>>> st_hm_plus = hm_plus.entrySet();
//		for (Entry<String, Vector<String>> map_entry:st_hm_plus) {
//			System.out.print("hm_plus[" + map_entry.getKey()+"]: "); 
//			System.out.println(map_entry.getValue() + " (id)"); 
//		}
		
//		// TH: displays hm_minus.
//		Set<Entry<String, Vector<String>>> st_hm_minus = hm_minus.entrySet();
//		for (Entry<String, Vector<String>> map_entry:st_hm_minus) {
//			System.out.print("hm_minus[" + map_entry.getKey()+"]: "); 
//			System.out.println(map_entry.getValue() + " (id)"); 
//		}
		
//		System.out.println("Kill Switch Activated."); System.exit(1);
		
		// TH: gets ready to solve the problem.
		unique_sets.clear();
		
		// TH: collects arc consistency checking algorithm solutions.
		Vector<Vector<Integer>> sol_arc_ = new Vector<Vector<Integer>>(); 
		
		// TH: holds on to what still needs to be pulled.
		Vector<Integer> toBePulled = new Vector<Integer>();
		
		{
			// TH: updates hm_plus_int_.
			Set< Map.Entry< String,Vector<String>> >st_hm_plus_temp_ = hm_plus.entrySet();
			for (Map.Entry< String,Vector<String>> map_entry_plus:st_hm_plus_temp_) {
				String key_=map_entry_plus.getKey();
				Vector<String> vector_ = map_entry_plus.getValue();
//				System.out.println("hm_plus[" + key_ + "]: " + vector_);
				Vector<Integer> vector_int_ = new Vector<Integer>(); 
				for(int j=0; j<vector_.size(); j++) {
					vector_int_.add(hm.get(vector_.get(j)));
				}//for j.
				Collections.sort(vector_int_);
//				System.out.println("vector_int_: " + vector_int_);
				hm_plus_int_.put(hm.get(key_), vector_int_);
			}// for st_hm_plus_temp_.
		}
		
//		{
//			// TH: displays hm_plus_int_.
//			Set< Map.Entry< Integer,Vector<Integer>> >st_hm_plus_temp_ = hm_plus_int_.entrySet();
//			for (Map.Entry< Integer,Vector<Integer>> map_entry_plus:st_hm_plus_temp_) {
//				Integer key_=map_entry_plus.getKey();
//				Vector<Integer> vector_ = map_entry_plus.getValue();
//				System.out.println("hm_plus_int_[" + key_ + "]: " + vector_);
//			}// for st_hm_plus_temp_.
//		}
		
		{
			// TH: updates hm_minus_int_.
			Set< Map.Entry< String,Vector<String>> >st_hm_minus_temp_ = hm_minus.entrySet();
			for (Map.Entry< String,Vector<String>> map_entry_minus:st_hm_minus_temp_) {
				String key_=map_entry_minus.getKey();
				Vector<String> vector_ = map_entry_minus.getValue();
//				System.out.println("hm_minus[" + key_ + "]: " + vector_);
				Vector<Integer> vector_int_ = new Vector<Integer>(); 
				for(int j=0; j<vector_.size(); j++) {
					vector_int_.add(hm.get(vector_.get(j)));
				}//for j.
				Collections.sort(vector_int_);
//				System.out.println("vector_int_: " + vector_int_);
				hm_minus_int_.put(hm.get(key_), vector_int_);
			}// for st_hm_minus_temp_.
		}
		
//		{
//			// TH: displays hm_minus_int_.
//			Set< Map.Entry< Integer,Vector<Integer>> >st_hm_minus_temp_ = hm_minus_int_.entrySet();
//			for (Map.Entry< Integer,Vector<Integer>> map_entry_minus:st_hm_minus_temp_) {
//				Integer key_=map_entry_minus.getKey();
//				Vector<Integer> vector_ = map_entry_minus.getValue();
//				System.out.println("hm_minus_int_[" + key_ + "]: " + vector_);
//			}// for st_hm_minus_temp_.
//		}
		
		// TH: initializes hm_plus_counter_ for Comparator sorting.
		for(int i=0; i<original_.size(); i++) {
			hm_plus_counter_.put((Integer)i, (Integer) 0);
		}//for i.
		
		// TH: updates hm_plus_counter_ for Comparator sorting, for both taker and giver.
		for(int i=0; i<original_.size()-1; i++) {
			Integer taker_ = original_.get(i);
			for(int j=i+1; j<original_.size(); j++) {
				Integer giver_ = original_.get(j);
				if(checkCompat(taker_, giver_)) {
					hm_plus_counter_.put(taker_,hm_plus_counter_.get(taker_)+1);
					hm_plus_counter_.put(giver_,hm_plus_counter_.get(giver_)+1);
				}//if checkCompat.
			}//for j.
		}//for i.
		
//		{
//			// TH: displays hm_plus_counter_.
//			Set< Map.Entry< Integer,Integer> >st_hm_plus_counter_ = hm_plus_counter_.entrySet();
//			for (Map.Entry< Integer,Integer> map_entry_plus:st_hm_plus_counter_) {
//				Integer key_=map_entry_plus.getKey();
//				Integer vector_ = map_entry_plus.getValue();
//				System.out.println("hm_plus_counter_[" + key_ + "]: " + vector_);
//			}// for st_hm_minus_temp_.
//		}
		
		// TH: tracks what have already been pulled.
		Vector<Integer> hasBeenPulled = new Vector<Integer>();
		
		// TH: loads initial problem set.
		unique_sets.add((Vector<Integer>) original_.clone());
		int original_size_ = original_.size();
		for(int i=0; i<original_size_; i++) toBePulled.add(original_.get(i));
		
		// TH: removes loners first (hm_plus_counter_); updates unique_sets, toBePulled, hasBeenPulled.
		{
			Vector<Integer> temp_ = unique_sets.get(0);
			Set< Map.Entry< Integer,Integer> >st_hm_plus_counter_ = hm_plus_counter_.entrySet();
			for (Map.Entry< Integer,Integer> map_entry_plus:st_hm_plus_counter_) {
				Integer loner_=map_entry_plus.getKey();
				Integer value_ = map_entry_plus.getValue();
//				System.out.println("hm_plus_counter_[" + key_ + "]: " + vector_);
				if(value_==0) {
					temp_.remove((Integer)loner_);
					toBePulled.remove((Integer)loner_);
					hasBeenPulled.add((Integer)loner_);
					Vector<Integer> temp_bag_ = new Vector<Integer>();
					temp_bag_.add(loner_);
					sol_arc_.add(temp_bag_);
				}
			}// for st_hm_minus_temp_.
		}
		
		Collections.sort(hasBeenPulled);
		Collections.sort(toBePulled, new MinusSort());
		
//		System.out.println("Kill Switch Activated."); System.exit(1);
		
//		System.out.print("unique_sets (Critical): "); 	System.out.println(unique_sets);
//		System.out.print("unique_sets.size       : "); 	System.out.println(unique_sets.size());
		
//		System.out.print("toBePulled (Critical): ");	 	System.out.println(toBePulled);
//		System.out.print("toBePulled.size       : "); 		System.out.println(toBePulled.size());
		
//		System.out.print("sol_arc_ (Critical): "); 		System.out.println(sol_arc_);
//		System.out.print("sol_arc_.size       : "); 		System.out.println(sol_arc_.size());
		
		for(int i=0; i<unique_sets.size(); i++) {
			
			// TH: presents items to be collected from.
			Vector<Integer> unique_sets_each_ = unique_sets.get(i);
			Collections.sort(unique_sets_each_, new MinusSort());
			
			// TH: helps iterate over elements in each set by weight constraints.
			Vector<Integer> unique_sets_each_original_ = new Vector<Integer>();
			for(int j=0; j<unique_sets_each_.size(); j++) {
				unique_sets_each_original_.add(unique_sets_each_.get(j));
			}//for j.
			
			// TH: sorts by weight, with most weight at index 0.
			Collections.sort(unique_sets_each_original_, new WeightSort());
			
			// TH: helps iterate over elements in each set by compatibility constraints.
			Vector<Integer> unique_sets_each_original_compat_ = new Vector<Integer>();
			for(int j=0; j<unique_sets_each_original_.size(); j++) {
				unique_sets_each_original_compat_.add(unique_sets_each_original_.get(j));
			}//for j.
			
			// TH: sorts by hm_minus constraints, with most constraints at index 0.
			Collections.sort(unique_sets_each_original_compat_, new MinusSort());
			
//			System.out.println("unique_sets_each_original_compat_ (Initial): " + unique_sets_each_original_compat_);
//			System.out.println("unique_sets_each_ (before): " + unique_sets_each_);
			
			// TH: helps reduce unique_sets_each_ once items have been collected into their bags.
			Vector<Integer> toBeRemoved = new Vector<Integer>();
			boolean nothingAdded = true;
			
			labelJ:
			for(int j=0; j<unique_sets_each_original_.size(); j++) {
				Integer taker_ = unique_sets_each_original_.get(j);
				if(hasBeenPulled.contains(taker_)) continue labelJ;
				
				Integer total_per_bag_ = hm_weight.get(hm_reverse.get(taker_));
				Vector<Integer> bag_ = new Vector<Integer>();
				
				Vector<Integer> bag_Helper_ = new Vector<Integer>();
				bag_Helper_.add(taker_);
				
				Vector<Integer> toBeRemoved_Helper_ = new Vector<Integer>();
				Vector<Integer> hasBeenPulled_Helper_ = new Vector<Integer>(); 
				labelK:
				for(int k=0; k<unique_sets_each_original_compat_.size(); k++) {
					
					Integer giver_ = unique_sets_each_original_compat_.get(k);
					if(giver_ == taker_) continue labelK;
					
					// TH: checks for weight constraint.
					if(total_per_bag_+hm_weight.get(hm_reverse.get(giver_))>sizebags) continue labelK;
					
					if(hasBeenPulled.contains(giver_)) continue labelK;
					if(hasBeenPulled_Helper_.contains(giver_)) continue labelK;
					
					// TH: checks if guess can go into house of single host.
					if(bag_Helper_.size()==1) {						
						if(!checkCompat(taker_, giver_)) continue labelK;
					}//if size==1.
					
					// TH: checks if guess can go into house of multiple hosts.
					for(int n=0; n<bag_Helper_.size(); n++) {
						Integer host_ = bag_Helper_.get(n);
						if(!checkCompat(host_, giver_)) continue labelK;
					}//for n.
					
					total_per_bag_+=hm_weight.get(hm_reverse.get(giver_));
					toBeRemoved_Helper_.add(giver_);
					hasBeenPulled_Helper_.add(giver_);
					bag_Helper_.add(giver_);
					nothingAdded = false;
					if(total_per_bag_==sizebags) break labelK;
					
				}//for k.
				
				if(bag_Helper_.size()>1) {
					if(total_per_bag_<=sizebags) {
						
						// TH: transfers from Helper_ over and adds to solution.
						
						toBeRemoved.add(taker_);
						for(int h=0; h<toBeRemoved_Helper_.size(); h++) {
							toBeRemoved.add(toBeRemoved_Helper_.get(h));
						}//for h.
						
						// TH: tracks what have been pulled.
						hasBeenPulled.add(taker_);
						for(int h=0; h<hasBeenPulled_Helper_.size(); h++) {
							hasBeenPulled.add(hasBeenPulled_Helper_.get(h));
						}//for h.
						
						// TH: loads each bag with compatible items.
						for(int h=0; h<bag_Helper_.size(); h++) {
							bag_.add(bag_Helper_.get(h));
						}//for h.
						
						{
							HashSet<Integer> hs1 = new HashSet<Integer>(hasBeenPulled); 
							hasBeenPulled = new Vector<Integer>(hs1);
						}
//						Collections.sort(hasBeenPulled);
						
						// TH: loads items into collection.
						sol_arc_.add((Vector<Integer>)bag_.clone());
						
						// TH: tracks what else still needs to be pulled.
						for(int b=0; b<bag_.size(); b++) {
							if(toBePulled.contains(bag_.get(b))) {
								toBePulled.remove((Integer) bag_.get(b));
							}//if toBePulled.
						}//for b.
						
					}
				}//if size>1.
				
			}//for j.
			
//			if(nothingAdded) {;}
			
			// TH: updates unique sets once items have been pulled out (i.e. collected in solutions).
			while(toBeRemoved.size()>0) {
				Integer purge_ = toBeRemoved.remove(0);
				unique_sets_each_.remove(purge_);
			}//while toBeRemoved>0.
			
			{
				HashSet<Integer> hs1 = new HashSet<Integer>(hasBeenPulled); 
				hasBeenPulled = new Vector<Integer>(hs1);
			}
//			Collections.sort(hasBeenPulled);
			
			for(int s=0; s<sol_arc_.size(); s++) {
				Vector<Integer> reference_ = sol_arc_.get(s);
				for(int u=0; u<unique_sets_each_.size(); u++) {
					Integer target_ = unique_sets_each_.get(u);
					if(reference_.contains(target_)) toBeRemoved.add(target_);
				}//for u.
			}//for s.
			
			while(toBeRemoved.size()>0) {
				Integer purge_ = toBeRemoved.remove(0);
				unique_sets_each_.remove(purge_);
			}//while toBeRemoved>0.
			
//			System.out.println("unique_sets_each_ (after): " + unique_sets_each_);
//			System.out.println("hasBeenPulled: " + hasBeenPulled);
//			System.out.println("toBePulled: " + toBePulled);
//			System.out.println("sol_arc_: " + sol_arc_);
			
			int toBePulled_size_ = toBePulled.size();
			int unique_sets_each_size_ = unique_sets_each_.size();
			
			// TH: if more bags used than allowed.
			if(sol_arc_.size()>numbags) {
//				System.out.println("sol_arc_: " + sol_arc_);
//				System.out.println("sol_arc_.size: " + sol_arc_.size());
				System.out.println("failure"); System.exit(1);
			}//if size>numbags-1.
			
			/** IMPORTANT: considers generating alternative universe here. */
			
			// TH: if there is only 1 loner item left to be bagged.
			if(toBePulled_size_==unique_sets_each_size_) {
				if(toBePulled_size_==1) {
					Integer loner_ = toBePulled.remove(0);
					Vector<Integer> loner_bag_ = new Vector<Integer>();
					loner_bag_.add(loner_);
					sol_arc_.add((Vector<Integer>)loner_bag_.clone());
					 
//					System.out.println("sol_arc_: " + sol_arc_);
//					System.out.println("sol_arc_.size: " + sol_arc_.size());
					
					displaySolution(sol_arc_);
					if(doItOnce) System.exit(0);
					
				}
			}//if only 1 loner item left to be bagged.
			
			// TH: attempts to find the last bag(s) in the arc consistency checking algorithm.
			labelW:
			while(toBePulled_size_ == unique_sets_each_size_){
				
				// TH: helps iterate over elements in each set by weight constraints.
				unique_sets_each_original_ = new Vector<Integer>();
				for(int j=0; j<unique_sets_each_.size(); j++) {
					unique_sets_each_original_.add(unique_sets_each_.get(j));
				}//for j.

//				// TH: sorts by weight, with most weight at index 0.
//				Collections.sort(unique_sets_each_original_, new WeightSort());
				
//				System.out.println("unique_sets_each_ (before, last scoop): " + unique_sets_each_);
				
				// TH: helps iterate over elements in each set by compatibility constraints.
				unique_sets_each_original_compat_ = new Vector<Integer>();
				for(int j=0; j<unique_sets_each_original_.size(); j++) {
					unique_sets_each_original_compat_.add(unique_sets_each_original_.get(j));
				}//for j.
				
//				// TH: sorts by hm_minus constraints, with most constraints at index 0.
//				Collections.sort(unique_sets_each_original_compat_, new MinusSort());
				
				toBeRemoved = new Vector<Integer>();
				nothingAdded = true;
				
				labelJ1:
				for(int j1=0; j1<unique_sets_each_original_.size(); j1++) {
					Integer taker_ = unique_sets_each_original_.get(j1);
					if(hasBeenPulled.contains(taker_)) continue labelJ1;
					
					Integer total_per_bag_ = hm_weight.get(hm_reverse.get(taker_));
					Vector<Integer> bag_ = new Vector<Integer>();
					
					Vector<Integer> bag_Helper_ = new Vector<Integer>();
					bag_Helper_.add(taker_);
					
					Vector<Integer> toBeRemoved_Helper_ = new Vector<Integer>();
					Vector<Integer> hasBeenPulled_Helper_ = new Vector<Integer>(); 
					labelK:
					for(int k=0; k<unique_sets_each_original_compat_.size(); k++) {
						
						Integer giver_ = unique_sets_each_original_compat_.get(k);
						if(giver_ == taker_) continue labelK;
						
						// TH: checks for weight constraint.
						if(total_per_bag_+hm_weight.get(hm_reverse.get(giver_))>sizebags) continue labelK;
						
						if(hasBeenPulled.contains(giver_)) continue labelK;
						if(hasBeenPulled_Helper_.contains(giver_)) continue labelK;
						
						if(bag_Helper_.size()==1) {
							if(!checkCompat(taker_, giver_)) continue labelK;
						}//if size==1.
						
						for(int n=0; n<bag_Helper_.size(); n++) {
							Integer host_ = bag_Helper_.get(n);
							if(!checkCompat(host_, giver_)) continue labelK;
						}//for n.
						
						total_per_bag_+=hm_weight.get(hm_reverse.get(giver_));
						toBeRemoved_Helper_.add(giver_);
						hasBeenPulled_Helper_.add(giver_);
						bag_Helper_.add(giver_);
						nothingAdded = false;
						if(total_per_bag_==sizebags) break labelK;
						
					}//for k.
					
					if(bag_Helper_.size()>1) {
						if(total_per_bag_<=sizebags) {
							
							// TH: transfers from Helper_ over and adds to solution.
							
							toBeRemoved.add(taker_);
							for(int h=0; h<toBeRemoved_Helper_.size(); h++) {
								toBeRemoved.add(toBeRemoved_Helper_.get(h));
							}//for h.
							
							hasBeenPulled.add(taker_);
							for(int h=0; h<hasBeenPulled_Helper_.size(); h++) {
								hasBeenPulled.add(hasBeenPulled_Helper_.get(h));
							}//for h.
							
							for(int h=0; h<bag_Helper_.size(); h++) {
								bag_.add(bag_Helper_.get(h));
							}//for h.
							
							{
								HashSet<Integer> hs1 = new HashSet<Integer>(hasBeenPulled); 
								hasBeenPulled = new Vector<Integer>(hs1);
							}
//							Collections.sort(hasBeenPulled);
							
							sol_arc_.add((Vector<Integer>)bag_.clone());
							
//							System.out.println("sol_arc_: " + sol_arc_);
//							System.out.println("sol_arc_.size: " + sol_arc_.size());
							
							for(int b=0; b<bag_.size(); b++) {
								if(toBePulled.contains(bag_.get(b))) {
									toBePulled.remove((Integer) bag_.get(b));
								}//if toBePulled.
							}//for b.
							
							if(toBePulled.size()==0){
								 
//								System.out.println("sol_arc_: " + sol_arc_);
//								System.out.println("sol_arc_.size: " + sol_arc_.size());
								
								displaySolution(sol_arc_);
								if(doItOnce) System.exit(0);
								
							}//if size==0.
							
						}
					}//if size>1.
					
				}//for j1.
				
				if(nothingAdded) break labelW;
				
				while(toBeRemoved.size()>0) {
					Integer purge_ = toBeRemoved.remove(0);
					unique_sets_each_.remove(purge_);
				}//while toBeRemoved>0.
				
				{
					HashSet<Integer> hs1 = new HashSet<Integer>(hasBeenPulled); 
					hasBeenPulled = new Vector<Integer>(hs1);
				}
//				Collections.sort(hasBeenPulled);
				
				for(int s=0; s<sol_arc_.size(); s++) {
					Vector<Integer> reference_ = sol_arc_.get(s);
					for(int u=0; u<unique_sets_each_.size(); u++) {
						Integer target_ = unique_sets_each_.get(u);
						if(reference_.contains(target_)) toBeRemoved.add(target_);
					}//for u.
				}//for s.
				
				while(toBeRemoved.size()>0) {
					Integer purge_ = toBeRemoved.remove(0);
					unique_sets_each_.remove(purge_);
				}//while toBeRemoved>0.
				
//				System.out.println("unique_sets_each_ (after, last scoop): " + unique_sets_each_);
//				System.out.println("hasBeenPulled: " + hasBeenPulled);
//				System.out.println("toBePulled: " + toBePulled);
//				System.out.println("sol_arc_: " + sol_arc_);
				
				toBePulled_size_ = toBePulled.size();
				unique_sets_each_size_ = unique_sets_each_.size();
				
			}// while toBePulled_size_ == unique_sets_each_size_.
			
//			System.out.println("unique_sets_each_original_compat_ (Final): " + unique_sets_each_original_compat_);
			
		}//for i.
		
		int toBePulled_initial_ = toBePulled.size();
		int toBePulled_later_  = toBePulled.size();
		
		labelW2:
		while(toBePulled.size()!=0) {
			
			toBePulled_initial_ = toBePulled.size();
			
			Vector<Integer> unique_sets_each_ = new Vector<Integer>();
			for(int i=0; i<toBePulled.size(); i++){
				unique_sets_each_.add(toBePulled.get(i));
			}// for i.
			
			// TH: helps iterate over elements in each set by weight constraints.
			Vector<Integer> unique_sets_each_original_ = new Vector<Integer>();
			for(int j=0; j<unique_sets_each_.size(); j++) {
				unique_sets_each_original_.add(unique_sets_each_.get(j));
			}//for j.

//			// TH: sorts by weight, with most weight at index 0.
//			Collections.sort(unique_sets_each_original_, new WeightSort());
			
			// TH: helps iterate over elements in each set by compatibility constraints.
			Vector<Integer> unique_sets_each_original_compat_ = new Vector<Integer>();
			for(int j=0; j<unique_sets_each_original_.size(); j++) {
				unique_sets_each_original_compat_.add(unique_sets_each_original_.get(j));
			}//for j.

//			// TH: sorts by hm_minus constraints, with most constraints at index 0.
//			Collections.sort(unique_sets_each_original_compat_, new MinusSort());
			
//			System.out.println("unique_sets_each_ (before): " + unique_sets_each_);
			
			Vector<Integer> toBeRemoved = new Vector<Integer>();
			boolean nothingAdded = true;
			
			labelJ:
			for(int j=0; j<unique_sets_each_original_.size(); j++) {
				Integer taker_ = unique_sets_each_original_.get(j);
				if(hasBeenPulled.contains(taker_)) continue labelJ;
				
				Integer total_per_bag_ = hm_weight.get(hm_reverse.get(taker_));
				Vector<Integer> bag_ = new Vector<Integer>();
				
				Vector<Integer> bag_Helper_ = new Vector<Integer>();
				bag_Helper_.add(taker_);
				
				Vector<Integer> toBeRemoved_Helper_ = new Vector<Integer>();
				Vector<Integer> hasBeenPulled_Helper_ = new Vector<Integer>(); 
				labelK:
				for(int k=0; k<unique_sets_each_original_compat_.size(); k++) {
					
					Integer giver_ = unique_sets_each_original_compat_.get(k);
					if(giver_ == taker_) continue labelK;
					
					// TH: checks for weight constraint.
					if(total_per_bag_+hm_weight.get(hm_reverse.get(giver_))>sizebags) continue labelK;
					
					if(hasBeenPulled.contains(giver_)) continue labelK;
					if(hasBeenPulled_Helper_.contains(giver_)) continue labelK;
					
					if(bag_Helper_.size()==1) {
						if(!checkCompat(taker_, giver_)) continue labelK;
					}//if size==1.
					
					for(int n=0; n<bag_Helper_.size(); n++) {
						Integer host_ = bag_Helper_.get(n);
						if(!checkCompat(host_, giver_)) continue labelK;
					}//for n.
					
					total_per_bag_+=hm_weight.get(hm_reverse.get(giver_));
					toBeRemoved_Helper_.add(giver_);
					hasBeenPulled_Helper_.add(giver_);
					bag_Helper_.add(giver_);
					nothingAdded = false;
					if(total_per_bag_==sizebags) break labelK;
					
				}//for k.
				
				if(bag_Helper_.size()>1) {
					if(total_per_bag_<=sizebags) {
						
						// TH: transfers from Helper_ over and adds to solution.
						
						toBeRemoved.add(taker_);
						for(int h=0; h<toBeRemoved_Helper_.size(); h++) {
							toBeRemoved.add(toBeRemoved_Helper_.get(h));
						}//for h.
						
						hasBeenPulled.add(taker_);
						for(int h=0; h<hasBeenPulled_Helper_.size(); h++) {
							hasBeenPulled.add(hasBeenPulled_Helper_.get(h));
						}//for h.
						
						for(int h=0; h<bag_Helper_.size(); h++) {
							bag_.add(bag_Helper_.get(h));
						}//for h.
						
						{
							HashSet<Integer> hs1 = new HashSet<Integer>(hasBeenPulled); 
							hasBeenPulled = new Vector<Integer>(hs1);
						}
//						Collections.sort(hasBeenPulled);
						
						sol_arc_.add((Vector<Integer>)bag_.clone());
						
						for(int b=0; b<bag_.size(); b++) {
							if(toBePulled.contains(bag_.get(b))) {
								toBePulled.remove((Integer) bag_.get(b));
							}//if toBePulled.
						}//for b.
						
					}
				}//if size>1.
				
			}//for j.
			
//			if(nothingAdded) {;}
			
			while(toBeRemoved.size()>0) {
				Integer purge_ = toBeRemoved.remove(0);
				unique_sets_each_.remove(purge_);
			}//while toBeRemoved>0.
			
			{
				HashSet<Integer> hs1 = new HashSet<Integer>(hasBeenPulled); 
				hasBeenPulled = new Vector<Integer>(hs1);
			}
//			Collections.sort(hasBeenPulled);
			
			for(int s=0; s<sol_arc_.size(); s++) {
				Vector<Integer> reference_ = sol_arc_.get(s);
				for(int u=0; u<unique_sets_each_.size(); u++) {
					Integer target_ = unique_sets_each_.get(u);
					if(reference_.contains(target_)) toBeRemoved.add(target_);
				}//for u.
			}//for s.
			
			while(toBeRemoved.size()>0) {
				Integer purge_ = toBeRemoved.remove(0);
				unique_sets_each_.remove(purge_);
			}//while toBeRemoved>0.
			
//			System.out.println("unique_sets_each_ (after): " + unique_sets_each_);
//			System.out.println("hasBeenPulled: " + hasBeenPulled);
//			System.out.println("toBePulled: " + toBePulled);
//			System.out.println("sol_arc_: " + sol_arc_);
			
			int toBePulled_size_ = toBePulled.size();
			int unique_sets_each_size_ = unique_sets_each_.size();
			
			// TH: if more bags used than allowed.
			if(sol_arc_.size()>numbags) {
//				System.out.println("sol_arc_: " + sol_arc_);
//				System.out.println("sol_arc_.size: " + sol_arc_.size());
				System.out.println("failure"); System.exit(1);
			}//if size>numbags-1.
			
			/** IMPORTANT: considers generating alternative universe here. */
			
			// TH: if there is only 1 loner item left to be bagged.
			if(toBePulled_size_==unique_sets_each_size_) {
				if(toBePulled_size_==1) {
					Integer loner_ = toBePulled.remove(0);
					Vector<Integer> loner_bag_ = new Vector<Integer>();
					loner_bag_.add(loner_);
					sol_arc_.add((Vector<Integer>)loner_bag_.clone());
					 
//					System.out.println("sol_arc_: " + sol_arc_);
//					System.out.println("sol_arc_.size: " + sol_arc_.size());
					
					displaySolution(sol_arc_);
					if(doItOnce) System.exit(0);
				}
			}//if only 1 loner item left to be bagged.
			
			// TH: attempts to find the last bag(s) in the arc consistency checking algorithm.
			labelW:
			while(toBePulled_size_ == unique_sets_each_size_){
				
				// TH: helps iterate over elements in each set by weight constraints.
				unique_sets_each_original_ = new Vector<Integer>();
				for(int j=0; j<unique_sets_each_.size(); j++) {
					unique_sets_each_original_.add(unique_sets_each_.get(j));
				}//for j.

//				// TH: sorts by weight, with most weight at index 0.
//				Collections.sort(unique_sets_each_original_, new WeightSort());
				
				// TH: helps iterate over elements in each set by compatibility constraints.
				unique_sets_each_original_compat_ = new Vector<Integer>();
				for(int j=0; j<unique_sets_each_original_.size(); j++) {
					unique_sets_each_original_compat_.add(unique_sets_each_original_.get(j));
				}//for j.

//				// TH: sorts by hm_minus constraints, with most constraints at index 0.
//				Collections.sort(unique_sets_each_original_compat_, new MinusSort());
				
//				System.out.println("unique_sets_each_ (before, last scoop): " + unique_sets_each_);
				
				toBeRemoved = new Vector<Integer>();
				nothingAdded = true;
				
				labelJ1:
				for(int j1=0; j1<unique_sets_each_original_.size(); j1++) {
					Integer taker_ = unique_sets_each_original_.get(j1);
					if(hasBeenPulled.contains(taker_)) continue labelJ1;
					
					Integer total_per_bag_ = hm_weight.get(hm_reverse.get(taker_));
					Vector<Integer> bag_ = new Vector<Integer>();
					
					Vector<Integer> bag_Helper_ = new Vector<Integer>();
					bag_Helper_.add(taker_);
					
					Vector<Integer> toBeRemoved_Helper_ = new Vector<Integer>();
					Vector<Integer> hasBeenPulled_Helper_ = new Vector<Integer>(); 
					labelK:
					for(int k=0; k<unique_sets_each_original_compat_.size(); k++) {
						
						Integer giver_ = unique_sets_each_original_.get(k);
						if(giver_ == taker_) continue labelK;
						
						// TH: checks for weight constraint.
						if(total_per_bag_+hm_weight.get(hm_reverse.get(giver_))>sizebags) continue labelK;
						
						if(hasBeenPulled.contains(giver_)) continue labelK;
						if(hasBeenPulled_Helper_.contains(giver_)) continue labelK;
						
						if(bag_Helper_.size()==1) {
							if(!checkCompat(taker_, giver_)) continue labelK;
						}//if size==1.
						
						for(int n=0; n<bag_Helper_.size(); n++) {
							Integer host_ = bag_Helper_.get(n);
							if(!checkCompat(host_, giver_)) continue labelK;
						}//for n.
						
						total_per_bag_+=hm_weight.get(hm_reverse.get(giver_));
						toBeRemoved_Helper_.add(giver_);
						hasBeenPulled_Helper_.add(giver_);
						bag_Helper_.add(giver_);
						nothingAdded = false;
						if(total_per_bag_==sizebags) break labelK;
						
					}//for k.
					
					if(bag_Helper_.size()>1) {
						if(total_per_bag_<=sizebags) {
							
							// TH: transfers from Helper_ over and adds to solution.
							
							toBeRemoved.add(taker_);
							for(int h=0; h<toBeRemoved_Helper_.size(); h++) {
								toBeRemoved.add(toBeRemoved_Helper_.get(h));
							}//for h.
							
							hasBeenPulled.add(taker_);
							for(int h=0; h<hasBeenPulled_Helper_.size(); h++) {
								hasBeenPulled.add(hasBeenPulled_Helper_.get(h));
							}//for h.
							
							for(int h=0; h<bag_Helper_.size(); h++) {
								bag_.add(bag_Helper_.get(h));
							}//for h.
							
							{
								HashSet<Integer> hs1 = new HashSet<Integer>(hasBeenPulled); 
								hasBeenPulled = new Vector<Integer>(hs1);
							}
//							Collections.sort(hasBeenPulled);
							
							sol_arc_.add((Vector<Integer>) bag_.clone());
							
//							System.out.println("sol_arc_: " + sol_arc_);
//							System.out.println("sol_arc_.size: " + sol_arc_.size());
							
							for(int b=0; b<bag_.size(); b++) {
								if(toBePulled.contains(bag_.get(b))) {
									toBePulled.remove((Integer) bag_.get(b));
								}//if toBePulled.
							}//for b.
							
							if(toBePulled.size()==0){
								 
//								System.out.println("sol_arc_: " + sol_arc_);
//								System.out.println("sol_arc_.size: " + sol_arc_.size());
								
								displaySolution(sol_arc_);
								if(doItOnce) System.exit(0);
								
							}//if size==0.
							
						}
					}//if size>1.
					
				}//for j1.
				
				if(nothingAdded) break labelW;
				
				while(toBeRemoved.size()>0) {
					Integer purge_ = toBeRemoved.remove(0);
					unique_sets_each_.remove(purge_);
				}//while toBeRemoved>0.
				
				{
					HashSet<Integer> hs1 = new HashSet<Integer>(hasBeenPulled); 
					hasBeenPulled = new Vector<Integer>(hs1);
				}
//				Collections.sort(hasBeenPulled);
				
				for(int s=0; s<sol_arc_.size(); s++) {
					Vector<Integer> reference_ = sol_arc_.get(s);
					for(int u=0; u<unique_sets_each_.size(); u++) {
						Integer target_ = unique_sets_each_.get(u);
						if(reference_.contains(target_)) toBeRemoved.add(target_);
					}//for u.
				}//for s.
				
				while(toBeRemoved.size()>0) {
					Integer purge_ = toBeRemoved.remove(0);
					unique_sets_each_.remove(purge_);
				}//while toBeRemoved>0.
				
//				System.out.println("unique_sets_each_ (after, last scoop): " + unique_sets_each_);
//				System.out.println("hasBeenPulled: " + hasBeenPulled);
//				System.out.println("toBePulled: " + toBePulled);
//				System.out.println("sol_arc_: " + sol_arc_);
				
				toBePulled_size_ = toBePulled.size();
				unique_sets_each_size_ = unique_sets_each_.size();
				
			}// while toBePulled_size_ == unique_sets_each_size_.
			
			toBePulled_later_  = toBePulled.size();
			
			if(toBePulled_initial_==toBePulled_later_) {
				break labelW2;
			}
			
		}//while toBePulled.size
		
		/** IMPORTANT: attempts to combine QP with GA. */
		
//		System.out.println("hm_plus: " + hm_plus);
		
		if(toBePulled.size() + sol_arc_.size()>numbags) {
//			System.out.println("sol_arc_: " + sol_arc_);
//			System.out.println("sol_arc_.size: " + sol_arc_.size());
//			System.out.println("toBePulled: " + toBePulled);
			
			Collections.sort(toBePulled, new MinusSort());
			
			// TH: Map of groupId and len(each vector of vectors of vectors).
			Map< Integer,Integer> hm_vect_vect_vect_all_len = new HashMap< Integer,Integer>();
			
			// TH: stores all possible compatible combinations in a vector of vectors of vectors.
			Vector<Vector<Vector<Integer>>> vect_vect_vect_all = new Vector<Vector<Vector<Integer>>>();
			
			Vector<Integer> toBeRemove_FromToBePulled = new Vector<Integer>();
			
			{
				/** IMPORTANT: generates alternative universe (i.e. mirrors of current universe). */
				
				// TH: specifies group id (i.e. only compatible items).
				int group=0;
				

				// TH: collects alternative pairs.
				Vector<Vector<Vector<Integer>>> vect_vect_vect = new Vector<Vector<Vector<Integer>>>();
				
				// TH: appends to each group in sol_arc, i.e. keeps original bag and adds loners to this bag.
				Vector<Integer> toBePulled_SubSet = new Vector<Integer>();
				
				for(int i=0; i<sol_arc_.size(); i++) {
					
					toBePulled_SubSet.clear();
					vect_vect_vect.clear();
					
					Vector<Integer> vect = sol_arc_.get(i);
					for(int j=0; j<vect.size(); j++) {
						
						Integer temp_Int = vect.get(j);
						
						labelK:
						for(int k=0; k<toBePulled.size(); k++) {
							
							// TH: finds another partner.
							Integer partner_ = toBePulled.get(k);
							
							// TH: filters by weight first.
							if(hm_weight.get(hm_reverse.get(temp_Int))+hm_weight.get(hm_reverse.get(partner_))>sizebags) continue labelK;
							
							// TH: filters by compatibility.
							Vector<Integer> alt_pair_ = new Vector<Integer>();
							alt_pair_.add(temp_Int); alt_pair_.add(partner_); 
							
							if(!checkCompat(temp_Int, partner_)) continue labelK;
							
							// TH: collects loners at the end of each group.
							toBePulled_SubSet.add(partner_);
							
							// TH: keeps remainder of original bag after one item has escaped.
							Vector<Integer> vect_temp_ = new Vector<Integer>();
							for(int t=0; t<vect.size(); t++) vect_temp_.add(vect.get(t));
							vect_temp_.remove(temp_Int);
							
							// TH: builds alternative universe.
							Vector<Vector<Integer>> vect_vect = new Vector<Vector<Integer>>(); 
							vect_vect.add(alt_pair_); vect_vect.add(vect_temp_);
							
							// TH: adds one collection of bags to each group.
							vect_vect_vect.add(vect_vect);
							
						}// for k.
						
					}//for j.
					
					// TH: keeps original universe and appends loners from alternative universe.
					{
						Vector<Vector<Integer>> vect_vect = new Vector<Vector<Integer>>();
						Vector<Integer> vect_temp_ = new Vector<Integer>();
						for(int t=0; t<vect.size(); t++) vect_temp_.add(vect.get(t));
						vect_vect.add((Vector<Integer>)vect_temp_.clone());
//						System.out.println("toBePulled_SubSet: " + toBePulled_SubSet);
						for(int t=0; t<toBePulled_SubSet.size(); t++) {
							Vector<Integer> loner_ = new Vector<Integer>();
							loner_.add(toBePulled_SubSet.get(t));
							toBeRemove_FromToBePulled.add(toBePulled_SubSet.get(t));
							vect_vect.add(loner_);
						}
//						System.out.println("vect_vect: " + vect_vect);
						// TH: adds one collection of bags to each group.
						vect_vect_vect.add(vect_vect);
					}
					
					for(int m=0; m<vect_vect_vect.size()-1; m++) {
						Vector<Vector<Integer>> vect_vect_ = vect_vect_vect.get(m);
						
						labelN:
						for(int n=0; n<toBePulled_SubSet.size(); n++) {
							Integer loner_ = toBePulled_SubSet.get(n);
							
							for(int k=0; k<vect_vect_.size(); k++) {
								Vector<Integer> vect_ = vect_vect_.get(k);
								if(vect_.contains(loner_)) continue labelN;
							}//for k.
							
							Vector<Integer> loner_vect_ = new Vector<Integer>();
							loner_vect_.add((Integer)loner_);
							vect_vect_.add(loner_vect_);
							
						}//for n.
						
					}//for m.
					
					
//					System.out.println("vect_vect_vect: " + vect_vect_vect);
//					System.out.println("toBePulled_SubSet: " + toBePulled_SubSet);
					
					for(int p=0; p<vect_vect_vect.size(); p++) {
						vect_vect_vect_all.add((Vector<Vector<Integer>>) vect_vect_vect.get(p).clone());
					}// for p.
					
					hm_vect_vect_vect_all_len.put((Integer)group,vect_vect_vect.size()); group++;
					
				}//for i.
				
			}
			
//			for(int i=0; i<vect_vect_vect_all.size(); i++) {
//				System.out.println("vect_vect_vect_all: " + vect_vect_vect_all.get(i));
//			}//for i.
			
//			System.out.println("hm_vect_vect_vect_all_len: " + hm_vect_vect_vect_all_len);
			
			Set< Map.Entry< Integer,Integer> > st_hm_vect_vect_vect_all_len = hm_vect_vect_vect_all_len.entrySet();
			
			// TH: tracks #items previously added back to vect_vect_vect_all before generating new combos.
			Integer group_length_previous=-1;
			
			labelC:
			for (Map.Entry< Integer,Integer> map_entry_len:st_hm_vect_vect_vect_all_len) {
				
				Integer group_position_ = map_entry_len.getKey();
				Integer group_length_   = map_entry_len.getValue();
				
//				if(group_position_==5) {
//					System.out.println("Wait Here 5 !! ");
//				}
				
				// TH: stores transits from original set.
				Vector<Vector<Vector<Integer>>> vect_vect_vect_all_transit = new Vector<Vector<Vector<Integer>>> ();
				for(int i=0; i<group_length_; i++) {
//					System.out.println("vect_vect_vect_all (before, remove(0)): " + vect_vect_vect_all);
					Vector<Vector<Integer>> vect_vector_temp_ = vect_vect_vect_all.remove(0);
//					System.out.println("vect_vect_vect_all (after, remove(0)): " + vect_vect_vect_all);
					vect_vect_vect_all_transit.add(vect_vector_temp_);
				}// for i.
				
				// TH: if very 1st transit, simply adds back to vect_vect_vect_all (i.e. stack-wise).
				if( group_position_==0) {
					Iterator<Vector<Vector<Integer>>> itr = vect_vect_vect_all_transit.iterator();
					while(itr.hasNext()) {
						Vector<Vector<Integer>> temp_ =  new Vector<Vector<Integer>>();
						temp_ = itr.next();
						vect_vect_vect_all.add(temp_);
					}// while hasNext.
					group_length_previous = group_length_;
					continue labelC;
				}// if group_position_==0.
				
				// TH: stores combinations, see below.
				Vector<Vector<Vector<Integer>>> vect_vect_vect_all_combo = new Vector<Vector<Vector<Integer>>> ();
				
				/** IMPORTANT: consider expanding vect_vect_vect_all_combo into more dimensions in order to find all possible solutions. */
				
				while(group_length_previous>0) {
					
					// TH: removes from top of stack, ready to be combined with some else.
					Vector<Vector<Integer>> vect_vect_temp_ = vect_vect_vect_all.remove(vect_vect_vect_all.size()-1);
					
					// TH: iterates over transit in order to generate combinations, see below.
					Iterator<Vector<Vector<Integer>>> itr = vect_vect_vect_all_transit.iterator();
					
					while(itr.hasNext()) {
						
						// TH: each combination in transit.
						Vector<Vector<Integer>> transit_vect_vect_ = itr.next();
						
						// TH: gets a clone of transit, to be added to template.
						Vector<Vector<Integer>> transit_vect_vect_clone = new Vector<Vector<Integer>>();
						for(int i=0; i<transit_vect_vect_.size(); i++) {
							transit_vect_vect_clone.add((Vector<Integer>) transit_vect_vect_.get(i).clone());
						}//for i.
						
						// TH: gets a clone of template.
						Vector<Vector<Integer>> vect_vect_temp_clone = new Vector<Vector<Integer>>();
						for(int i=0; i<vect_vect_temp_.size(); i++) {
							vect_vect_temp_clone.add((Vector<Integer>) vect_vect_temp_.get(i).clone());
						}//for i.
						
						// TH: appends transit to template.
						for(int i=0; i<transit_vect_vect_clone.size(); i++) {
							vect_vect_temp_clone.add((Vector<Integer>) transit_vect_vect_clone.get(i).clone());
						}//for i.
						
						// TH: adds to combo collection.
						vect_vect_vect_all_combo.add(vect_vect_temp_clone);
						
					}// while itr.hasNext.
					
					--group_length_previous;
					
				}// while group_length_previous>0.
				
				// TH: updates previous length, i.e. how many recently generated combos, see below.
				group_length_previous = vect_vect_vect_all_combo.size();
				
				if(group_position_==hm_vect_vect_vect_all_len.size()-1) {
					
					// TH: removes items from toBePulled, i.e. already loaded into candidate bags.
					{
						HashSet<Integer> hs1 = new HashSet<Integer>(toBeRemove_FromToBePulled); 
						toBeRemove_FromToBePulled = new Vector<Integer>(hs1);
					}
//					System.out.println("toBeRemove_FromToBePulled: " + toBeRemove_FromToBePulled);
//					System.out.println("toBePulled (before): " + toBePulled);
					while(toBeRemove_FromToBePulled.size()>0) {
						toBePulled.remove(toBeRemove_FromToBePulled.remove(0));
					}
//					System.out.println("toBePulled (after): " + toBePulled);
					
//					System.out.println("vect_vect_vect_all_combo.size: " + vect_vect_vect_all_combo.size());
//					for(int i=0; i<vect_vect_vect_all_combo.size(); i++) {
//						System.out.println("vect_vect_vect_all_combo: " + vect_vect_vect_all_combo.get(i));
//					}
					
					// TH: displays each combo set.
					labelC1:
					for(int c=0; c<vect_vect_vect_all_combo.size(); c++) {
						Vector<Vector<Integer>> candidate_ = vect_vect_vect_all_combo.get(c);
						
						for(int i=0; i<candidate_.size(); i++) Collections.sort(candidate_.get(i));
						
						// TH: absorbs smaller vectors by using larger vectors, forward pass.
						Vector<Vector<Integer>> toBeRemoved = new Vector<Vector<Integer>>();
						{
							labelI:
							for(int i=0; i<candidate_.size()-1; i++) {
									Vector<Integer> reference_ = candidate_.get(i);
									for(int j=i+1; j<candidate_.size(); j++) {
										Vector<Integer> target_ = candidate_.get(j);
										if(reference_.containsAll(target_)) {
											toBeRemoved.add(target_);
											continue labelI;
										}//if containsAll.
									}//for j.
							}//for i.
							while(toBeRemoved.size()>0) {
								candidate_.remove(toBeRemoved.remove(0));
							}//while toBeRemoved.size>0.
						}
						
						// TH: absorbs smaller vectors by using larger vectors, backward pass.
						{
							labelI:
							for(int i=candidate_.size()-1; i>0; i--) {
									Vector<Integer> reference_ = candidate_.get(i);
									for(int j=i-1; j>=0; j--) {
										Vector<Integer> target_ = candidate_.get(j);
										if(reference_.containsAll(target_)) {
											toBeRemoved.add(target_);
											continue labelI;
										}//if containsAll.
									}//for j.
							}//for i.
							while(toBeRemoved.size()>0) {
								candidate_.remove(toBeRemoved.remove(0));
							}//while toBeRemoved.size>0.
						}
						
//						System.out.println("candidate_: " + candidate_);
						
						// TH: if a solution has been found at this point.
						if(candidate_.size() + toBePulled.size()<=numbags) {
							
							// TH: adds loners to solution set.
							for(int i=0; i<toBePulled.size(); i++) {
								Integer loner_ = toBePulled.get(i);
								Vector<Integer> loner_vect_ = new Vector<Integer>();
								loner_vect_.add(loner_);
								candidate_.add(loner_vect_);
							}//for i.
							
							Vector<Integer> collected_ = new Vector<Integer>();
							for(int i=0; i<candidate_.size(); i++){
								Vector<Integer> candidate_each_ = candidate_.get(i);
								Integer candidate_each_size_ = candidate_each_.size();
								for(int j=0; j<candidate_each_size_; j++) {
									collected_.add((Integer)candidate_each_.get(j));
								}//for j.
							}//for i.
							
							// TH: avoids duplicates in collected_.
							{
								HashSet<Integer> hs1 = new HashSet<Integer>(collected_); 
								collected_ = new Vector< Integer>(hs1);
							}
							Collections.sort(collected_);
							
							// TH: if NOT all elements made it to this point.
							if(collected_.size()!=original_.size()){
								continue labelC1;
							}
							// TH: displays solution from arc consistency checking.
							else {
								
								displaySolution(candidate_);
								if(doItOnce) System.exit(0);
								
							}
							
						}//if <=numbags.
						
						// TH: collects candidate solutions in alternative universe, by merging singles together.
						Vector<Vector<Vector<Integer>>> candidate_alt_ = new Vector<Vector<Vector<Integer>>>();
						
						labelI:
						for(int i=0; i<candidate_.size()-1; i++) {
							Vector<Integer> candidate_taker_ =  candidate_.get(i);
							if(candidate_taker_.size()>1) continue labelI;
							
							labelJ:
							for(int j=i+1; j<candidate_.size(); j++) {
								Vector<Integer> candidate_giver_ =  candidate_.get(j);
								if(candidate_giver_.size()>1) continue labelJ;
								
								Integer candidate_taker_value_ = candidate_taker_.get(0);
								Integer candidate_giver_value_ = candidate_giver_.get(0);
								
								// TH: filters by weight first.
								if(hm_weight.get(hm_reverse.get(candidate_taker_value_))+hm_weight.get(hm_reverse.get(candidate_giver_value_))>sizebags) continue labelJ;
								
								// TH: filters by compatibility.
								if(!checkCompat(candidate_taker_value_, candidate_giver_value_)) continue labelJ;
								
								{
									// TH: constructs an alternative candidate solution.
									Vector<Vector<Integer>> candidate_alt_each_ = new Vector<Vector<Integer>>();
									for(int i1=0; i1<candidate_.size(); i1++) {
										candidate_alt_each_.add((Vector<Integer>) candidate_.get(i1).clone());
									}//for i.
									
									Vector<Integer> candidate_each_taker_ =  candidate_alt_each_.get(i);
									candidate_each_taker_.add(candidate_giver_value_);
									
									candidate_alt_each_.remove(j);
									
//									System.out.println("candidate_alt_each_: " + candidate_alt_each_);
//									System.out.println("candidate_ (again): " + candidate_);
									
									candidate_alt_.add(candidate_alt_each_);
									
								}
								
							}//for j.
						}//for i.
						
						for(int i=0; i<candidate_alt_.size(); i++) {
							
							Vector<Vector<Integer>> candidate_alt_each_ = candidate_alt_.get(i);
							if(candidate_alt_each_.size() + toBePulled.size()<=numbags) {
								
								// TH: adds loners to solution set.
								for(int j=0; j<toBePulled.size(); j++) {
									Integer loner_ = toBePulled.get(j);
									Vector<Integer> loner_vect_ = new Vector<Integer>();
									loner_vect_.add(loner_);
									candidate_alt_each_.add(loner_vect_);
								}//for i.
								
//								System.out.println("candidate_alt_each_ (again): " + candidate_alt_each_);
								
								// TH: displays a solution from alternative universe and then exits.
								{
									
									displaySolution(candidate_alt_each_);
									if(doItOnce) System.exit(0);
									
								}
								
							}//if within numbags.
							
						}//for i.
						
					}//for c.
					
					// TH: if more processing still, goes here.
					
				}
				
				// TH: adds all combinations back to vect_vect_vect_all, stack-wise.
				//		this code seems to be a little redundant.
				while(vect_vect_vect_all_combo.size()>0) {
					vect_vect_vect_all.add(vect_vect_vect_all_combo.remove(0));
				}// while combo.size>0.
				
			}// while map_entry_len.
			
//			for(int i=0; i<vect_vect_vect_all.size(); i++) {
//				Vector<Vector<Integer>> temp_ = vect_vect_vect_all.get(i);
//				System.out.println("vect_vect_vect_all: " + temp_);
//			}// for i.
//			System.out.println("vect_vect_vect_all.size: " + vect_vect_vect_all.size());
			
			// TH: adds all combinations back to vect_vect_vect_all, stack-wise.
			//		this code seems to be a little redundant.
			
			System.out.println("failure"); System.exit(1);
			
		}//if more bags than allocated for.
		
		// TH: spreads out as single-item bags, i.e. cheesy solution.
		while(toBePulled.size()>0) {
			Integer loner_ = toBePulled.remove(0);
			Vector<Integer> loner_bag_ = new Vector<Integer> ();
			loner_bag_.add(loner_);
			sol_arc_.add((Vector<Integer>) loner_bag_.clone());
		}//while toBePulled.size
		
//		System.out.println("sol_arc_: " + sol_arc_);
//		System.out.println("sol_arc_.size: " + sol_arc_.size());
		
		// TH: displays solution from arc consistency checking.
		{
			
			displaySolution(sol_arc_);
			if(doItOnce) System.exit(0);
			
		}
		
		// TH: K & R - Exercise 6.2
		// builds tree (AVL) for optimization.
		
		// TH: Input Arguments::
		// 	C:\Users\tungh\eclipse-workspace\CS457-P1\P1_\src\test_X3_.txt  -depth
		if(mode.equals("-depth")) {
			
			boolean solution = false;
			
			if(!solution) {
//				System.out.println("sol_arc_: " + sol_arc_);
//				System.out.println("sol_arc_.size: " + sol_arc_.size());
				System.out.println("failure"); System.exit(1);
			}
			System.exit(0);
			
		}// depth-first search.
		
		// TH: Input Arguments::
		// 	C:\Users\tungh\eclipse-workspace\CS457-P1\P1_\src\test_X3_.txt  -breadth
		else if(mode.equals("-breadth")) {
			
			boolean solutions = false;
			
			if(!solutions) {
				System.out.println("failure"); System.exit(1);
			}
			System.exit(0);
			
		}// breadth-first search.
		
	}//main.
	
	private static boolean checkCompat(Integer taker_, Integer giver_) {
		
		// TH: returns true if compatible, false otherwise.
		
		// TH: if taker_ accepts anything.
		if(hm_plus_int_.get(taker_).size()>0){
			// TH: if taker accepts giver.
			if(Collections.binarySearch(hm_plus_int_.get(taker_),giver_)>=0) {
				// TH: if giver accepts anything.
				if(hm_plus_int_.get(giver_).size()>0){
					// TH: if giver accepts taker.
					if(Collections.binarySearch(hm_plus_int_.get(giver_),taker_)>=0) return true;
					// TH: if giver does NOT accept taker.
					else return false;
				}
				// TH: if giver rejects anything.
				else {
					// TH: if giver rejects taker.
					if(Collections.binarySearch(hm_minus_int_.get(giver_),taker_)>=0) return false;
					else return true;
				}
			}
			// TH: if taker does NOT accept giver.
			else return false; 
		}
		// TH: if taker rejects anything.
		else {
			// TH: if taker rejects giver
			if(Collections.binarySearch(hm_minus_int_.get(taker_),giver_)>=0) return false;
			// TH: if taker does NOT reject giver.
			else {
				
				// TH: if giver accepts anything.
				if(hm_plus_int_.get(giver_).size()>0){
					// TH: if giver accepts taker.
					if(Collections.binarySearch(hm_plus_int_.get(giver_),taker_)>=0) return true;
					// TH: if giver does NOT accept taker.
					else return false;
				}
				// TH: if giver rejects anything.
				else {
					// TH: if giver rejects taker.
					if(Collections.binarySearch(hm_minus_int_.get(giver_),taker_)>=0) return false;
					else return true;
				}
				
			}
		}
		
	}//checkCompat.

	private static void displaySolution(Vector<Vector<Integer>> sol_arc_) {
		
		check_simple_0(sol_arc_);
		Iterator<Vector<Integer>> each_solution_itr = sol_arc_.iterator();
		System.out.println("success");
		
		labelF:
		while(each_solution_itr.hasNext()) {
			
			Vector<Integer> bag_temp_ = each_solution_itr.next();
			if(bag_temp_.size()==1) {
				System.out.println(hm_reverse.get(bag_temp_.get(0)));
				continue labelF;
			}//if size==1.
			
			// TH: iterates over multiple items per bag.
			Iterator<Integer> itr_multiple = bag_temp_.iterator();
			while(itr_multiple.hasNext()) {
				System.out.print(hm_reverse.get(itr_multiple.next()));
				if(itr_multiple.hasNext()) System.out.print("\t");
			}// while itr_multiple.
			System.out.println();
			
		}// while each_solution_itr.
		
//		System.out.println("sol_arc_.size: " + sol_arc_.size());
		if(doItOnce) System.exit(0);
		
	}//displaySolution.

	// TH: checks to make sure that all items have been collected.
	private static void check_simple_0(Vector<Vector<Integer>> sol_arc_) {
		
		Vector<Integer> original_comp_ = new Vector<Integer>();
		for(int i=0; i<sol_arc_.size(); i++) {
			Vector<Integer> sol_arc_each_ = sol_arc_.get(i);
			for(int j=0; j<sol_arc_each_.size(); j++) {
				original_comp_.add(sol_arc_each_.get(j));
			}//for j.
		}//for i.
		
//		System.out.println("original_comp_.size (before): " + original_comp_.size());
		// TH: avoids duplicates in original_comp_.
		// https://way2java.com/collections/vector/removing-duplicates-from-vector/
		{
			HashSet<Integer> hs1 = new HashSet<Integer>(original_comp_); 
			original_comp_ = new Vector< Integer>(hs1);
		}
		Collections.sort(original_comp_);
//		System.out.println("original_comp_.size (after): " + original_comp_.size());
		
		if(original_comp_.size()!=original_.size()) {
			System.out.println("failure");
//			System.out.println("original_: \t\t" + original_);
//			System.out.println("original_comp_: \t" + original_comp_);
			System.exit(1);
		}
		
	}//check_simple_0.
	
	static class LengthSort implements Comparator<Vector<Integer>> {
		
		@Override
	    public int compare(Vector<Integer> a, Vector<Integer> b) {
			
			// TH: longest sets on left.
			if( a.size() > b.size() ) return -1;
			else if( a.size() < b.size() ) return 1;
			return 0;
			
		}//compare.
		
	}// LengthSort.
	
	static class MinusSort implements Comparator<Integer> {
		
		@Override
	    public int compare(Integer a, Integer b) {
			
			// TH: least positive constrainted items on left.
			if( hm_plus_counter_.get(a) > hm_plus_counter_.get(b) ) return 1;
			else if( hm_plus_counter_.get(a) < hm_plus_counter_.get(b) ) return -1;
			else {
				// TH: heaviest items on left.
				if(hm_weight.get(hm_reverse.get(a)) > hm_weight.get(hm_reverse.get(b))) return -1;
				else if(hm_weight.get(hm_reverse.get(a)) < hm_weight.get(hm_reverse.get(b))) return 1;
				else return 0;
			}
			
//			// TH: most negative constrainted items on left.
//			if( hm_minus.get(hm_reverse.get(a)).size() > hm_minus.get(hm_reverse.get(b)).size() ) return -1;
//			else if( hm_minus.get(hm_reverse.get(a)).size() < hm_minus.get(hm_reverse.get(b)).size() ) return 1;
//			return 0;
			
		}//compare.
		
	}// MinusSort.
	
	static class WeightSort implements Comparator<Integer> {
		
		@Override
	    public int compare(Integer a, Integer b) {
			
			// TH: least positive constrainted items on left.
			if( hm_plus_counter_.get(a) > hm_plus_counter_.get(b) ) return 1;
			else if( hm_plus_counter_.get(a) < hm_plus_counter_.get(b) ) return -1;
			else {
				// TH: heaviest items on left.
				if(hm_weight.get(hm_reverse.get(a)) > hm_weight.get(hm_reverse.get(b))) return -1;
				else if(hm_weight.get(hm_reverse.get(a)) < hm_weight.get(hm_reverse.get(b))) return 1;
				else return 0;
			}
			
//			// TH: heaviest items on left.
//			if(hm_weight.get(hm_reverse.get(a)) > hm_weight.get(hm_reverse.get(b))) return -1;
//			else if(hm_weight.get(hm_reverse.get(a)) < hm_weight.get(hm_reverse.get(b))) return 1;
//			else return 0;
			
		}//compare.
		
	}//WeightSort.
	
}//P1_Solver_v4_Arc_.
