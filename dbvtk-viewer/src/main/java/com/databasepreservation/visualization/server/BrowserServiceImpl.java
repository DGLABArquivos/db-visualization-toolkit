package com.databasepreservation.visualization.server;

import java.util.List;

import org.roda.core.data.exceptions.AuthenticationDeniedException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.utils.JsonUtils;
import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.v2.index.IsIndexed;
import org.roda.core.data.v2.index.facet.Facets;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.databasepreservation.visualization.client.BrowserService;
import com.databasepreservation.visualization.client.SavedSearch;
import com.databasepreservation.visualization.client.ViewerStructure.ViewerDatabase;
import com.databasepreservation.visualization.client.ViewerStructure.ViewerRow;
import com.databasepreservation.visualization.client.ViewerStructure.ViewerTable;
import com.databasepreservation.visualization.client.common.search.SearchField;
import com.databasepreservation.visualization.client.common.search.SearchInfo;
import com.databasepreservation.visualization.shared.BrowserServiceUtils;
import com.databasepreservation.visualization.utils.SolrUtils;
import com.databasepreservation.visualization.utils.UserUtility;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class BrowserServiceImpl extends RemoteServiceServlet implements BrowserService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BrowserServiceImpl.class);

  /**
   * Escape an html string. Escaping data received from the client helps to
   * prevent cross-site script vulnerabilities.
   *
   * @param html
   *          the html string to escape
   * @return the escaped string
   */
  private String escapeHtml(String html) {
    if (html == null) {
      return null;
    }
    return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  }

  public IndexResult<ViewerDatabase> findDatabases(Filter filter, Sorter sorter, Sublist sublist, Facets facets,
    String localeString) throws GenericException, AuthorizationDeniedException, RequestNotValidException {
    UserUtility.Authorization.allowIfAdmin(getThreadLocalRequest());
    return ViewerFactory.getSolrManager().find(ViewerDatabase.class, filter, sorter, sublist, facets);
  }

  public IndexResult<SavedSearch> findSavedSearches(String databaseUUID, Filter filter, Sorter sorter, Sublist sublist,
    Facets facets, String localeString) throws GenericException, AuthorizationDeniedException,
    RequestNotValidException, NotFoundException {
    UserUtility.Authorization
      .checkFilteringPermission(getThreadLocalRequest(), databaseUUID, filter, SavedSearch.class);
    return ViewerFactory.getSolrManager().find(SavedSearch.class, filter, sorter, sublist, facets);
  }

  @Override
  public <T extends IsIndexed> T retrieve(String databaseUUID, String classNameToReturn, String id)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    Class<T> classToReturn = parseClass(classNameToReturn);
    T result = ViewerFactory.getSolrManager().retrieve(classToReturn, id);
    UserUtility.Authorization.checkRetrievalPermission(getThreadLocalRequest(), databaseUUID, classToReturn, result);
    return result;
  }

  @Override
  public IndexResult<ViewerRow> findRows(String databaseUUID, String tableUUID, Filter filter, Sorter sorter,
    Sublist sublist, Facets facets, String localeString) throws GenericException, AuthorizationDeniedException,
    RequestNotValidException {
    try {
      ViewerDatabase database = ViewerFactory.getSolrManager().retrieve(ViewerDatabase.class, databaseUUID);
      UserUtility.Authorization.checkTableAccessPermission(getThreadLocalRequest(), database, tableUUID);
    } catch (NotFoundException e) {
      throw new RequestNotValidException("Invalid database UUID: " + databaseUUID, e);
    }

    return ViewerFactory.getSolrManager().findRows(ViewerRow.class, tableUUID, filter, sorter, sublist, facets);
  }

  @Override
  public Long countRows(String databaseUUID, String tableUUID, Filter filter) throws AuthorizationDeniedException,
    GenericException, RequestNotValidException {
    try {
      ViewerDatabase database = ViewerFactory.getSolrManager().retrieve(ViewerDatabase.class, databaseUUID);
      UserUtility.Authorization.checkTableAccessPermission(getThreadLocalRequest(), database, tableUUID);
    } catch (NotFoundException e) {
      throw new RequestNotValidException("Invalid database UUID: " + databaseUUID, e);
    }

    return ViewerFactory.getSolrManager().countRows(ViewerRow.class, tableUUID, filter);
  }

  @Override
  public ViewerRow retrieveRows(String databaseUUID, String tableUUID, String rowUUID)
    throws AuthorizationDeniedException, GenericException, NotFoundException {
    ViewerDatabase database = ViewerFactory.getSolrManager().retrieve(ViewerDatabase.class, databaseUUID);
    UserUtility.Authorization.checkTableAccessPermission(getThreadLocalRequest(), database, tableUUID);
    return ViewerFactory.getSolrManager().retrieveRows(ViewerRow.class, tableUUID, rowUUID);
  }

  @Override
  public String getSolrQueryString(Filter filter, Sorter sorter, Sublist sublist, Facets facets)
    throws GenericException, RequestNotValidException {
    // does not retrieve data from index => safe to ignore authorization
    return SolrUtils.getSolrQuery(filter, sorter, sublist, facets);
  }

  @Override
  public String saveSearch(String name, String description, String tableUUID, String tableName, String databaseUUID,
    SearchInfo searchInfo) throws AuthorizationDeniedException, GenericException, RequestNotValidException,
    NotFoundException {
    ViewerDatabase database = ViewerFactory.getSolrManager().retrieve(ViewerDatabase.class, databaseUUID);
    UserUtility.Authorization.checkTableAccessPermission(getThreadLocalRequest(), database, tableUUID);

    String searchInfoJson = JsonUtils.getJsonFromObject(searchInfo);

    SavedSearch savedSearch = new SavedSearch();
    savedSearch.setUUID(SolrUtils.randomUUID());
    savedSearch.setName(name);
    savedSearch.setDescription(description);
    savedSearch.setDatabaseUUID(databaseUUID);
    savedSearch.setTableUUID(tableUUID);
    savedSearch.setTableName(tableName);
    savedSearch.setSearchInfoJson(searchInfoJson);

    ViewerFactory.getSolrManager().addSavedSearch(savedSearch);

    return savedSearch.getUUID();
  }

  @Override
  public void editSearch(String databaseUUID, String savedSearchUUID, String name, String description)
    throws AuthorizationDeniedException, GenericException, RequestNotValidException, NotFoundException {
    // get the saved search
    SavedSearch savedSearch = ViewerFactory.getSolrManager().retrieve(SavedSearch.class, savedSearchUUID);
    // authorise viewing the saved search
    UserUtility.Authorization.checkSavedSearchPermission(getThreadLocalRequest(), databaseUUID, savedSearch);
    // authorise editing the saved search
    UserUtility.Authorization.allowIfAdminOrManager(getThreadLocalRequest());

    ViewerFactory.getSolrManager().editSavedSearch(savedSearchUUID, name, description);
  }

  @Override
  public void deleteSearch(String databaseUUID, String savedSearchUUID) throws AuthorizationDeniedException,
    GenericException, RequestNotValidException, NotFoundException {
    // get the saved search
    SavedSearch savedSearch = ViewerFactory.getSolrManager().retrieve(SavedSearch.class, savedSearchUUID);
    // authorise viewing the saved search
    UserUtility.Authorization.checkSavedSearchPermission(getThreadLocalRequest(), databaseUUID, savedSearch);
    // authorise editing the saved search
    UserUtility.Authorization.allowIfAdminOrManager(getThreadLocalRequest());

    ViewerFactory.getSolrManager().deleteSavedSearch(savedSearchUUID);
  }

  @Override
  public Boolean isAuthenticationEnabled() throws RODAException {
    return ViewerConfiguration.getInstance().getIsAuthenticationEnabled();
  }

  @Override
  public List<SearchField> getSearchFields(ViewerTable viewerTable) throws GenericException {
    // does not retrieve data from index => safe to ignore authorization
    return BrowserServiceUtils.getSearchFieldsFromTable(viewerTable);
  }

  @SuppressWarnings("unchecked")
  private <T extends IsIndexed> Class<T> parseClass(String classNameToReturn) throws GenericException {
    Class<T> classToReturn;
    try {
      classToReturn = (Class<T>) Class.forName(classNameToReturn);
    } catch (ClassNotFoundException e) {
      throw new GenericException("Could not find class " + classNameToReturn);
    }
    return classToReturn;
  }

  public User getAuthenticatedUser() throws RODAException {
    User user = UserUtility.getUser(this.getThreadLocalRequest());
    LOGGER.debug("Serving user {}", user);
    return user;
  }

  public User login(String username, String password) throws AuthenticationDeniedException, GenericException {
    User user = UserLoginController.login(username, password, this.getThreadLocalRequest());
    LOGGER.debug("Logged user {}", user);
    return user;
  }

  @Override
  public String uploadSIARD(String path) throws GenericException {
    return SIARDController.loadFromLocal(path);
  }

  @Override
  public ViewerDatabase uploadSIARDStatus(String databaseUUID) throws AuthorizationDeniedException, NotFoundException,
    GenericException {
    return retrieve(databaseUUID, ViewerDatabase.class.getName(), databaseUUID);
  }

  @Override
  public String getReport(String databaseUUID) throws GenericException, AuthorizationDeniedException, NotFoundException {
    ViewerDatabase dummy = new ViewerDatabase();
    dummy.setUuid(databaseUUID);
    UserUtility.Authorization.checkRetrievalPermission(getThreadLocalRequest(), databaseUUID, ViewerDatabase.class,
      dummy);
    return SIARDController.getReportFileContents(databaseUUID);
  }
}
