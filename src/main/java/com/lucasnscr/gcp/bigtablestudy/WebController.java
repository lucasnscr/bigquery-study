package com.lucasnscr.gcp.bigtablestudy;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.bigquery.core.BigQueryTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.lucasnscr.gcp.bigtablestudy.BigQuerySampleConfiguration.BigQueryFileGateway;

@Controller
public class WebController {

	@Autowired
	BigQueryFileGateway bigQueryFileGateway;

	@Autowired
	BigQueryTemplate bigQueryTemplate;

	@Value("${spring.cloud.gcp.bigquery.datasetName}")
	private String datasetName;

	@GetMapping("/")
	public ModelAndView renderIndex(ModelMap map) {
		map.put("datasetName", this.datasetName);
		return new ModelAndView("index.html", map);
	}

	/**
	 * Handles a file upload using {@link BigQueryTemplate}.
	 *
	 * @param file the CSV file to upload to BigQuery
	 * @param tableName name of the table to load data into
	 * @return ModelAndView of the response the send back to users
	 *
	 * @throws IOException if the file is unable to be loaded.
	 */
	@PostMapping("/uploadFile")
	public ModelAndView handleFileUpload(
			@RequestParam("file") MultipartFile file, @RequestParam("tableName") String tableName)
			throws IOException {

		ListenableFuture<Job> loadJob = this.bigQueryTemplate.writeDataToTable(
				tableName, file.getInputStream(), FormatOptions.csv());

		return getResponse(loadJob, tableName);
	}

	/**
	 * Handles CSV data upload using Spring Integration {@link BigQueryFileGateway}.
	 *
	 * @param csvData the String CSV data to upload to BigQuery
	 * @param tableName name of the table to load data into
	 * @return ModelAndView of the response the send back to users
	 */
	@PostMapping("/uploadCsvText")
	public ModelAndView handleCsvTextUpload(
			@RequestParam("csvText") String csvData, @RequestParam("tableName") String tableName) {

		ListenableFuture<Job> loadJob = this.bigQueryFileGateway.writeToBigQueryTable(csvData.getBytes(), tableName);

		return getResponse(loadJob, tableName);
	}

	private ModelAndView getResponse(ListenableFuture<Job> loadJob, String tableName) {
		String message;
		try {
			Job job = loadJob.get();
			message = "Successfully loaded data file to " + tableName;
		}
		catch (Exception e) {
			e.printStackTrace();
			message = "Error: " + e.getMessage();
		}

		return new ModelAndView("index")
				.addObject("datasetName", this.datasetName)
				.addObject("message", message);
	}
}
