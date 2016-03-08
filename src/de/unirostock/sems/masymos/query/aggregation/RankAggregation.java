package de.unirostock.sems.masymos.query.aggregation;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.unirostock.sems.masymos.configuration.RankAggregationType;
import de.unirostock.sems.masymos.query.results.ModelResultSet;
import de.unirostock.sems.masymos.util.ResultSetUtil;

public class RankAggregation {
		
	
	public static List<ModelResultSet> aggregate(List<List<ModelResultSet>> rankersList, List<ModelResultSet> aggregateRanker, RankAggregationType.Types type){

		
		if (aggregateRanker.isEmpty()) return aggregateRanker;
		
		switch(type){ 
			
		case ADJACENT_PAIRS: 
			return adj(rankersList, aggregateRanker); 
		case COMB_MNZ:
			return combMNZ(rankersList, aggregateRanker);
		case LOCAL_KEMENIZATION:
			return localKemenization(rankersList, aggregateRanker);
		case SUPERVISED_LOCAL_KEMENIZATION: 
			return supervisedLocalKemenization(rankersList, aggregateRanker);
		case DEFAULT: 
			return aggregateRanker;
		default: 
			return aggregateRanker;
		}
	}
	
	
	private static List<Integer> getRankersLengths(List<List<ModelResultSet>> rankersList){
		int s = rankersList.size();
		
		List<Integer> length_rankers = new LinkedList<Integer>();
		for(int i = 0; i < s; i++){
			length_rankers.add(rankersList.get(i).size());
		}
		return length_rankers;
	}
	
	private static int getMeanRankersLengths (List<List<ModelResultSet>> rankersList){
		int s = rankersList.size();
		List<Integer> length_rankers = getRankersLengths(rankersList);
		Collections.sort(length_rankers);
		return length_rankers.get(s/2);
	}
	
	private static int distance(int k, List<ModelResultSet> ranker_1, List<ModelResultSet> ranker_2){
		
		int sum = 0;
		double ranking1OfObject1;
		double ranking1OfObject2;
		double ranking2OfObject1;
		double ranking2OfObject2;
		ModelResultSet object1;
		ModelResultSet object2;
		
		for(int i = 0; i < Math.min(k, ranker_1.size()); i++)
			for(int j = 0; j <  Math.min(k, ranker_2.size()); j++){
				object1 = ranker_1.get(i);
				ranking1OfObject1 = i + 1; 
				
				if(ranker_2.contains(object1))
					ranking2OfObject1 = ranker_2.indexOf(object1) + 1;
				else{
					ranking2OfObject1 = k+1;
				}
				
				object2 = ranker_2.get(i);
				ranking2OfObject2 = j + 1; 
				
				if(ranker_1.contains(object2))
					ranking1OfObject2 = ranker_1.indexOf(object1) + 1;
				else{
					ranking1OfObject2 = k+1;
				}
				
				//In the case when object1, object2 both appear in one list and they are both absent in another -> no disagreement
				
				if(((ranking1OfObject1 - ranking1OfObject2) * (ranking2OfObject1 - ranking2OfObject2)) <= 0)
					sum++;
				}
		
			
		if(k > 0)	
			//the normalized distance 	
			sum = sum/(k * (k-1)/2);
		
		return sum;	
	}

	private static double distance_avg(List<List<ModelResultSet>> rankersList, List<ModelResultSet> aggregateRanker){
		int s = rankersList.size();
		double sum = 0;
		
		int mean_length = getMeanRankersLengths(rankersList);
		
		List<ModelResultSet> ranker_i = new LinkedList<ModelResultSet>();
		
		for(int i = 0; i < s; i++){
			ranker_i = rankersList.get(i);
			sum += distance(mean_length, ranker_i, aggregateRanker);
		}
		sum = sum / s;
		return sum;
	}

	private static void swap(List<ModelResultSet> ranker, int index1, int index2){
		ModelResultSet object1 = ranker.get(index1);
		ModelResultSet object2 = ranker.get(index2);
	
		ranker.set(index1, object2);
		ranker.set(index2, object1);
	}

	private static List<ModelResultSet> adj (List<List<ModelResultSet>> rankersList, List<ModelResultSet> aggregateRanker){ //adjacent pairs, based on Ke-tau
		double epsilon_min = Integer.MAX_VALUE;
		int count = 0;
		//boolean changed = false;
		
		while (count < 100){ //repeat for-loop until no further reductions can be performed
			//changed = false;
			for(int i = 0; i < aggregateRanker.size() - 2; i++){
				List<ModelResultSet> tempRanker = aggregateRanker;
				swap(tempRanker, i, i+1);
				double epsilon_av = distance_avg(rankersList, tempRanker);
				if (epsilon_av < epsilon_min){
					aggregateRanker = tempRanker;
					epsilon_min = epsilon_av;
					//changed = true;
				}	
			}
			/*
			if (changed){
				
				count = 0;
			}
			else*/ 
				count++;
		}
		return aggregateRanker;
	}
	
	private static List<ModelResultSet> combMNZ(List<List<ModelResultSet>> rankersList, List<ModelResultSet> aggregateRanker){
		int s = rankersList.size();
		
		if((aggregateRanker != null) && (!aggregateRanker.isEmpty())) {
			for (ModelResultSet o : aggregateRanker) {
				int indexOf_o = aggregateRanker.indexOf(o);
				int h = 0; //denotes the number of times object o appears in the rankers
				float brn_sum = 0; //Borda rank normalization for the object o

				for (int i = 0; i < s; i++) { //compute h
					List<ModelResultSet> ranker_i = rankersList.get(i);

					if (ranker_i.contains(o)) {
						int ranking_o = ranker_i.indexOf(o) + 1; //Indexshift
						h++;
						brn_sum += 1 - ((double) (ranking_o - 1) / aggregateRanker.size()); //update brn_i
					}
				}

				o.setScore(brn_sum * h);
				aggregateRanker.set(indexOf_o, o); //Replace the object (new score)
			}
		}
		
		ResultSetUtil.sortModelResultSetByScore(aggregateRanker);
		return aggregateRanker;
	}
	
	private static List<ModelResultSet> localKemenization(List<List<ModelResultSet>> rankersList, List<ModelResultSet> aggregateRanker){
		int rankersListLength = rankersList.size();
		int aggregateRankerLength = aggregateRanker.size();
		int rankingOfObject1;
		int rankingOfObject2;
		
		for(int i = 1; i < aggregateRankerLength; i++)
			for(int j = i-1; j >= 0; j--){
				int pro = 0;
				int con = 0;
				ModelResultSet object2 = aggregateRanker.get(i);
				ModelResultSet object1 = aggregateRanker.get(j);
				
				//Compare the rankings of object1 and object2 in each ranker 
				for(int l = 0; l < rankersListLength; l++){
					List<ModelResultSet> ranker_i = rankersList.get(l);
					
					if(ranker_i.contains(object1))
						rankingOfObject1 = ranker_i.indexOf(object1);
					else
						rankingOfObject1 = Integer.MAX_VALUE; 
					
					if(ranker_i.contains(object2))
						rankingOfObject2 = ranker_i.indexOf(object2);
					else
						rankingOfObject2 = Integer.MAX_VALUE; 
					
					//update pro if the ranking is the same as in the initial aggregate ranker
					//update cons otherwise
					if(rankingOfObject2 > rankingOfObject1)
						pro++;
					else
						con++;
				}
				
				//swap object1 and object2 if the majority of the rankers prefer object2 to object1 (if ranking of object 2 < ranking of object1)
				if(con > pro){
					swap(aggregateRanker, i, j);
				}
				else if (pro >= con)
					break;
				
			}
		return aggregateRanker;
	}
	
	private static List<ModelResultSet> supervisedLocalKemenization ( List<List<ModelResultSet>> rankersList, List<ModelResultSet> aggregateRanker){
		
		//to be out sourced
		HashMap<Integer, Integer> weights = new HashMap<Integer, Integer>();
		weights.put(0, 4);
		weights.put(1, 3);
		weights.put(2, 1);
		weights.put(3, 1);
		
		int numberOfRankers = rankersList.size();
		int aggregateRankerLength = aggregateRanker.size();
		boolean[][] M = new boolean[aggregateRankerLength][aggregateRankerLength];
	
		int rankingOfObject1;
		int rankingOfObject2;
		
		int weightsSum = 0;
		for(int o: weights.values())
			weightsSum += o;
	
		
		for(int i = 0; i < aggregateRankerLength; i++){
			for(int j = i + 1 ; j < aggregateRankerLength; j++){
				ModelResultSet object1 = aggregateRanker.get(i);
				ModelResultSet object2 = aggregateRanker.get(j);
				int score = 0;
				
				for(int l = 0; l < numberOfRankers; l++){
					List<ModelResultSet> ranker_i = rankersList.get(l);
					
					if(ranker_i.contains(object1))
						rankingOfObject1 = ranker_i.indexOf(object1) + 1;
					else
						rankingOfObject1 = Integer.MAX_VALUE; 
					
					if(ranker_i.contains(object2))
						rankingOfObject2 = ranker_i.indexOf(object2) + 1;
					else
						rankingOfObject2 = Integer.MAX_VALUE; 
					
					if(rankingOfObject1 > rankingOfObject2)
						score += weights.get(l);
				}
				
				if (score > 0.5 * weightsSum){
					M[i][j] = true;
					M[j][i] = false;
				}
				
			}
		}
		for(int x = 0; x < aggregateRankerLength; x++){
			for(int y = x + 1; y < aggregateRankerLength; y++){
				if(M[x][y] == false)
					swap(aggregateRanker, x, y);
			}
		}
		
		return aggregateRanker;
	}
	

}