<?php
include 'SpellCorrector.php';

ini_set('display_errors', 0);
ini_set('memory_limit', '1024M');

header('Content-Type: text/html; charset=utf-8');

$limit = 10; //Limit to 10 results
$query = isset($_REQUEST['q']) ? $_REQUEST['q'] : false; //Query
$results = false;    //Search resutls
$fileUrl = false;    //Mapping file id to url
$misspelled = false; //Is query misspelled?
$requery = isset($_REQUEST['requery']) ? $_REQUEST['requery'] : false; //Query w/o spelling correction

/* Return snippet
 * @param $query query
 * @param $description metadata-description
 * @param $fullId path and file name of the document
 */
function generateSnippet($query, $description, $fullId) {
	$dom = new DOMDocument;
	$dom->loadHTML(file_get_contents($fullId));
	
	$sentences = Array(); //store snippet candidates
	if ($description) array_push($sentences, $description); //if has description metadata
	foreach ($dom->getElementsByTagName('p') as $node) { //contents in <p> tags
		array_push($sentences, $node->nodeValue);
	}
	
	$map = Array(); //key: sentence containing query terms, value: position of the term in the sentence
	$token = strtok($query, " "); //one term at a time
	while ($token) {
		foreach ($sentences as $s) {
			$pos = stripos($s, $token);
			if ($pos && !array_key_exists($s, $map)) {
				$map[$s] = $pos;
				break; //found one, no need to continue
			}
		}
		$token = strtok(" ");
	}
	
	$keys = array_keys($map);
	$count = count($keys);
	$res = ""; //snippet result
	for ($i = 0; $i < $count; $i++) {
		$str = $keys[$i];
		$pos = $map[$str];
		$res .= ($pos > 100) ? substr($str, $pos - 50, 100) : substr($str, 0, 100); //at most 100 chars long
		if ($i < $count - 1) $res .= " ... "; //seperate by ...
	}
	return $res;
}

//Populate the map of file id to url
if (!$fileUrl) { 
	$fileUrl = array();
	$mapfile = fopen('mapNYTimesDataFile.csv', 'r') or die('Unable to open mapfile!');
	while (!feof($mapfile)) {
		$line = fgets($mapfile);
		$file = substr($line, 0, 41); //Length of file id is 41
		$url = substr($line, 42, strlen($line) - 42);
		$fileUrl[$file] = $url;
	}
	fclose($mapfile);
}

//If query is set, send query to Solr server
if ($query) {
	require_once('Apache/Solr/Service.php');	
	$solr = new Apache_Solr_Service('localhost', 8983, '/solr/myexample');	
	$additionalParameters = array('sort' => $_REQUEST['sort']);
	$query = trim($query);
	
	if (get_magic_quotes_gpc() == 1) {
		$query = stripslashes($query);
	}

	//Spelling correction
	if ($requery) { //If it's re-query (no spelling correction)
		$query_corrected = trim($query);
	} else {        //Normal query
		$query_corrected = "";
		$token = strtok($query, " ");
		while ($token !== false) {
			$query_corrected .= SpellCorrector::correct($token) . " ";
			$token = strtok(" ");
		}
		$query_corrected = trim($query_corrected);
		$misspelled = (strcmp(strtolower($query), strtolower($query_corrected)) !== 0) ? true : false;
	}

	//Send to solr server
	try	{
		$results = $solr->search($query_corrected, 0, $limit, $additionalParameters);
	} catch (Exception $e) {
		die("<html><head><title>SEARCH EXCEPTION</title><body><pre>{$e->__toString()}</pre></body></html>");
	}
}
?>
<html>
	<head><title>PHP Solr Client</title></head>
	<body>
		<!-- Search box -->
		<form accept-charset="utf-8" method="get">
			<label for="q">Search:</label>
			<input type="text" name="q" id="q" list="datalist" oninput="getSuggest()"
				   value="<?php echo htmlspecialchars($query, ENT_QUOTES, 'utf-8'); ?>" >
				<datalist id="datalist"></datalist>
			<input type="submit" value="Search" />
			<input name="sort" type="radio" value="score desc" <?php if (!isset($_REQUEST['sort']) || 
			(isset($_REQUEST['sort']) && $_REQUEST['sort'] === 'score desc')) echo "checked"; ?> />Solr Default
			<input name="sort" type="radio" value="pageRankFile desc" <?php	if (isset($_REQUEST['sort']) && 
			$_REQUEST['sort'] === 'pageRankFile desc') echo "checked"; ?> />PageRank<br />
		</form>

		<!-- Spelling correction section -->
		<?php
		if ($misspelled) {
			$sort = ($_REQUEST['sort'] === 'score desc') ? "score+desc" : "pageRankFile+desc";
		?>
		Showing results for <?php echo "<i><b>" . $query_corrected . "</b></i>"; ?><br />
		Search instead for <?php echo "<a href='http://localhost/mySolr/?q=" . $query 
		. "&sort=" . $sort . "&requery=true'>" . $query . "</a>"; ?><br /><br />
		<?php
		}		
		?>

		<!-- Result section -->
		<?php
		if ($results) {
			$total = (int) $results->response->numFound;
			$start = min(1, $total);
			$end = min($limit, $total);
			$q = $results->responseHeader->params->q;
		?>
		<div>Results <?php echo $start; ?> - <?php echo $end; ?> of <?php echo $total; ?>:</div>
		<ol>
			<?php
			foreach ($results->response->docs as $doc) {
				$title = $doc->title;
				$fullId = $doc->id;
				$id = substr($fullId, -41); //Length of file id is 41
				$url = $fileUrl[$id];
				$description = $doc->description;
				$snippet = generateSnippet($q, $description, $fullId);				
			?>
			<li>
				<a href="<?php echo $url; ?>" target="_blank"><b><?php echo $title; ?></b></a><br />
				<a href="<?php echo $url; ?>" target="_blank"><?php echo $url; ?></a><br />
				<?php if (strlen($snippet) !== 0) echo $snippet . "<br />"; ?>
				<br />
			</li>
			<?php
			}
			?>
		</ol>
		<?php
		}
		?>		
		<script type="text/javascript">
			//Auto-complete or auto-suugestion, use AJAX to get suggestions from solr server
			function getSuggest() {
				var val = document.getElementById("q").value; //get the current input in the search box
				var index = val.lastIndexOf(" "); //find the last space
				var prefix = (index === -1) ? "" : val.slice(0, index).trim(); //if no space, no prefix
				var query = val.substring(index + 1).toLowerCase(); //get the last word in the query
				var xhttp = new XMLHttpRequest();
				
				xhttp.onreadystatechange = function() {
					if (this.readyState == 4 && this.status == 200) {
						var obj = JSON.parse(this.responseText);
						var arr = obj.suggest.suggest[query].suggestions; //suggestion json objects
						
						var str = ""; //html string for options of datalist
						for (i in arr) {
							var term = arr[i].term; //suugestion term
							if (term.search(/[^a-zA-Z0-9]/) === -1) { //filter terms containing special chars
								str += "<option value='";
								str += prefix;
								if (prefix.length !== 0) str += " ";
								str += arr[i].term + "' />";
							}
						}
						document.getElementById("datalist").innerHTML = str; //insert html
					}
				};
				
				xhttp.open("GET", "http://localhost:8983/solr/myexample/suggest?wt=json&q=" + query, true);
				xhttp.send();
			}
		</script>
	</body>
</html>
