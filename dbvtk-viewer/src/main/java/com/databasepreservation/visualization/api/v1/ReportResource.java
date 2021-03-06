/**
 * The contents of this file are based on those found at https://github.com/keeps/roda
 * and are subject to the license and copyright detailed in https://github.com/keeps/roda
 */
package com.databasepreservation.visualization.api.v1;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.roda.core.data.exceptions.NotFoundException;

import com.databasepreservation.visualization.api.utils.ApiUtils;
import com.databasepreservation.visualization.api.utils.DownloadUtils;
import com.databasepreservation.visualization.server.ViewerConfiguration;
import com.databasepreservation.visualization.shared.ViewerSafeConstants;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Path(ReportResource.ENDPOINT)
@Api(value = ReportResource.SWAGGER_ENDPOINT)
public class ReportResource {
  public static final String ENDPOINT = ViewerSafeConstants.API_V1_REPORT_RESOURCE;
  public static final String SWAGGER_ENDPOINT = "v1 report";

  @GET
  @Path("/{" + ViewerSafeConstants.API_PATH_PARAM_DATABASE_UUID + "}")
  public Response getResource(
    @ApiParam(value = "The database uuid", required = true) @PathParam(ViewerSafeConstants.API_PATH_PARAM_DATABASE_UUID) String databaseUUID)
    throws IOException, NotFoundException {

    java.nio.file.Path reportPath = ViewerConfiguration.getInstance().getReportPath(databaseUUID);
    String filename = reportPath.getFileName().toString();
    if (!Files.exists(reportPath)) {
      throw new NotFoundException("Missing report file: " + filename);
    }

    InputStream reportStream = Files.newInputStream(reportPath);

    if (reportStream != null) {
      return ApiUtils.okResponse(DownloadUtils.getReportResourceStreamResponse(reportPath, reportStream));
    } else {
      throw new NotFoundException("Missing report file: " + filename);
    }
  }
}
