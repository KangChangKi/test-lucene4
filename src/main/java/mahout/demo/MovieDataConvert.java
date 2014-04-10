package mahout.demo;

public class MovieDataConvert {

	/**
	 * cat u.data | cut -f1,2,3 | tr "\\t" ","
	 * 
	 * ref: http://chimpler.wordpress.com/2013/02/20/playing-with-the-mahout-recommendation-engine-on-a-hadoop-cluster/
	 * 
	 * u.data: contains several tuples(user_id, movie_id, rating, timestamp)
	 * u.user: contains several tuples(user_id, age, gender, occupation, zip_code)
	 * u.item: contains several tuples(movie_id, title, release_date, video_release_data, imdb_url, cat_unknown, cat_action, cat_adventure, cat_animation, cat_children, cat_comedy, cat_crime, cat_documentary, cat_drama, cat_fantasy, cat_film_noir, cat_horror, cat_musical, cat_mystery, cat_romance, cat_sci_fi, cat_thriller, cat_war, cat_western)
	 * 
	 */
	public static void main(String[] args) {
		
	}
}
