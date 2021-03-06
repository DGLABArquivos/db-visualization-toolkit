/**
 * The contents of this file are based on those found at https://github.com/keeps/roda
 * and are subject to the license and copyright detailed in https://github.com/keeps/roda
 */
package com.databasepreservation.visualization.api.utils;

import com.databasepreservation.visualization.api.common.ConsumesOutputStream;
import com.databasepreservation.visualization.api.common.EntityResponse;

/**
 * @author Bruno Ferreira <bferreira@keep.pt>
 */
public class StreamResponse implements EntityResponse {
  private String filename;
  private String mediaType;
  private ConsumesOutputStream stream;

  public StreamResponse(String filename, String mediaType, ConsumesOutputStream stream) {
    super();
    this.filename = filename;
    this.mediaType = mediaType;
    this.stream = stream;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getMediaType() {
    return mediaType;
  }

  public void setMediaType(String mediaType) {
    this.mediaType = mediaType;
  }

  public ConsumesOutputStream getStream() {
    return stream;
  }

  public void setStream(ConsumesOutputStream stream) {
    this.stream = stream;
  }

}
