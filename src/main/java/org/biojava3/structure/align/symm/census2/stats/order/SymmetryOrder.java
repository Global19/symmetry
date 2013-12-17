package org.biojava3.structure.align.symm.census2.stats.order;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.biojava.bio.structure.scop.ScopDatabase;
import org.biojava.bio.structure.scop.ScopDescription;
import org.biojava.bio.structure.scop.ScopDomain;
import org.biojava.bio.structure.scop.ScopFactory;
import org.biojava3.structure.align.symm.census2.Result;
import org.biojava3.structure.align.symm.census2.Results;
import org.biojava3.structure.align.symm.census2.Significance;
import org.biojava3.structure.align.symm.census2.SignificanceFactory;
import org.biojava3.structure.align.symm.census2.stats.StatUtils;

/**
 * Tabulate symmetry by order.
 * 
 * @author dmyerstu
 */
public class SymmetryOrder {

	public static enum ExampleType {
		FOLD, SUPERFAMILY, FAMILY, DOMAIN;
	}

	public static class OrderInfo {
		private Map<String, Integer> nDomainsInFolds; // indexed by folds ONLY
		private Map<String, Set<String>> superfamilies; // indexed by classification (folds and superfamilies)
		private Map<String, Set<String>> families; // indexed by classification (folds and superfamilies)
		private Map<String, Set<String>> domains; // indexed by classification (folds, superfamilies, and families)
		private int order;

		public OrderInfo(int order) {
			this.order = order;
			nDomainsInFolds = new HashMap<String,Integer>();
			superfamilies = new HashMap<String, Set<String>>();
			families = new HashMap<String, Set<String>>();
			domains = new HashMap<String, Set<String>>();
		}

		public int getOrder() {
			return order;
		}

		public SortedSet<String> getDomainSet() {
			SortedSet<String> theSet = new TreeSet<String>(getComparator());
			for (Set<String> set : domains.values()) theSet.addAll(set);
			return theSet;
		}

		public SortedSet<String> getFamilySet() {
			SortedSet<String> theSet = new TreeSet<String>(getComparator());
			for (Set<String> set : families.values()) theSet.addAll(set);
			return theSet;
		}

		public SortedSet<String> getSuperfamilySet() {
			SortedSet<String> theSet = new TreeSet<String>(getComparator());
			for (Set<String> set : superfamilies.values()) {
				theSet.addAll(set);
			}
			return theSet;
		}

		private Comparator<String> getComparator() {
		return new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					if (!nDomainsInFolds.containsKey(o1) || !nDomainsInFolds.containsKey(o2))
						return o1.compareTo(o2);
					return nDomainsInFolds.get(o2).compareTo(nDomainsInFolds.get(o1));
				}
			};
		}
		
		public SortedSet<String> getFoldSet() {
			SortedSet<String> set = new TreeSet<String>(getComparator());
			for (String s : nDomainsInFolds.keySet()) set.add(s);
			return set;
		}

		public SortedMap<String, Integer> getnDomainsInFold() {
			Comparator<String> comp = new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					if (!nDomainsInFolds.containsKey(o1) || !nDomainsInFolds.containsKey(o2))
						return o1.compareTo(o2);
					return nDomainsInFolds.get(o2).compareTo(nDomainsInFolds.get(o1));
				}
			};
			SortedMap<String, Integer> counts = new TreeMap<String, Integer>(comp);
			counts.putAll(this.nDomainsInFolds);
			return counts;
		}

		public void plusFoldInstance(ScopDomain domain, ScopDatabase scop) {

			final String scopId = domain.getScopId();

			// determine fold
			int foldId = domain.getFoldId();
			ScopDescription foldDesc = scop.getScopDescriptionBySunid(foldId);
			String fold = foldDesc.getClassificationId();

			// determine superfamily
			int sfId = domain.getSuperfamilyId();
			ScopDescription sfDesc = scop.getScopDescriptionBySunid(sfId);
			String sf = sfDesc.getClassificationId();

			// determine family
			int familyId = domain.getFamilyId();
			ScopDescription famDesc = scop.getScopDescriptionBySunid(familyId);
			String family = famDesc.getClassificationId();

			StatUtils.plus(nDomainsInFolds, fold);

			// plus superfamilies
			StatUtils.plusSet(superfamilies, fold, sf);

			// plus families
			StatUtils.plusSet(families, fold, family);
			StatUtils.plusSet(families, sf, family);

			// plus domains
			StatUtils.plusSet(domains, fold, scopId);
			StatUtils.plusSet(domains, sf, scopId);
			StatUtils.plusSet(domains, family, scopId);
		}

		@Override
		public String toString() {
			return toString(5);
		}

		/**
		 * Prints information about folds that have the symmetry order. Lists up to {@code limit} folds, ordered by number of domains with that symmetry.
		 * @param limit The maximum number of folds to list.
		 * @return
		 */
		public String toString(int limit) {
			SortedMap<String,Integer> counts = getnDomainsInFold();
			StringBuilder sb = new StringBuilder();
			sb.append(getDomainSet().size() + " domains, " + getFamilySet().size() + " families, " + getSuperfamilySet().size() + " superfamilies, " + getFoldSet().size() + " folds for order=" + order + ":" + StatUtils.NEWLINE);
			int i = 0;
			sb.append("fold\tN domains\tN SFs" + StatUtils.NEWLINE);
			for (Map.Entry<String, Integer> entry : counts.entrySet()) {
				if (i < limit) {
					sb.append(entry.getKey() + "\t" + entry.getValue() + "\t" + superfamilies.get(entry.getKey()).size()
							+ StatUtils.NEWLINE);
				}
				i++;
			}
			return sb.toString();
		}
	}

	private static final Logger logger = LogManager.getLogger(SymmetryOrder.class.getName());

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: " + SymmetryOrder.class.getSimpleName() + " census-file.xml");
			return;
		}
		SymmetryOrder orders = new SymmetryOrder(Results.fromXML(new File(args[0])));
		System.out.println(orders);
		System.out.println(orders.toTable(ExampleType.SUPERFAMILY, 16, "\t", "" + StatUtils.NEWLINE, "\t"));
	}

	private Map<Integer, OrderInfo> orderInfos = new TreeMap<Integer, OrderInfo>();

	public SymmetryOrder(Results census) {
		ScopDatabase scop = ScopFactory.getSCOP(ScopFactory.VERSION_1_75A);
		Significance sig = SignificanceFactory.rotationallySymmetricSmart();
		for (Result result : census.getData()) {
			if (result.getAlignment() == null || result.getAxis() == null) {
				logger.warn("Skipping " + result.getScopId());
			}
			if (!sig.isSignificant(result))
				continue;
			int order;
			if (result.getOrder() != null && result.getOrder() > 1) {
				order = result.getOrder();
			} else {
				continue;
				//				order = result.getAxis().guessOrder();
			}
			ScopDomain domain = scop.getDomainByScopID(result.getScopId());
			if (domain == null) {
				logger.error(result.getScopId() + " is null");
			}
			if (!orderInfos.containsKey(order))
				orderInfos.put(order, new OrderInfo(order));
			orderInfos.get(order).plusFoldInstance(domain, scop);
		}
	}

	public String toTable(ExampleType exampleType, int exampleLimit, String tab, String newline, String comma) {
		StringBuilder sb = new StringBuilder();
		sb.append("order" + tab + "N folds" + tab + "N superfamilies" + tab + "N families" + tab + "N domains" + tab + "examples" + newline);
		for (OrderInfo info : orderInfos.values()) {
			sb.append(info.getOrder() + tab + info.getFoldSet().size() + tab + info.getSuperfamilySet().size() + tab + info.getFamilySet().size() + tab + info.getDomainSet().size() + tab);
			int i = 0;
			Set<String> examples = null;
			switch(exampleType) {
			case FOLD:
				examples = info.getFoldSet();
				break;
			case SUPERFAMILY:
				examples = info.getSuperfamilySet();
				break;
			case FAMILY:
				examples = info.getFamilySet();
				break;
			case DOMAIN:
				examples = info.getDomainSet();
				break;
			}
			for (String example : examples) {
				if (i >= exampleLimit) break;
				sb.append(example);
				i++;
				if (i < examples.size() && i < exampleLimit) sb.append(comma);
			}
			sb.append(newline);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (OrderInfo result : orderInfos.values()) {
			sb.append("====================== " + result.getOrder() + " ============================"
					+ StatUtils.NEWLINE);
			sb.append(result.toString(10));
			sb.append("=====================================================" + StatUtils.NEWLINE + StatUtils.NEWLINE);
		}
		return sb.toString();
	}

}