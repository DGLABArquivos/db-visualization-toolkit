package com.databasepreservation.visualization.client.browse;

import java.util.HashMap;
import java.util.Map;

import com.databasepreservation.visualization.client.ViewerStructure.ViewerCheckConstraint;
import com.databasepreservation.visualization.client.ViewerStructure.ViewerDatabase;
import com.databasepreservation.visualization.client.ViewerStructure.ViewerSchema;
import com.databasepreservation.visualization.client.ViewerStructure.ViewerTable;
import com.databasepreservation.visualization.client.common.lists.BasicTablePanel;
import com.databasepreservation.visualization.client.common.utils.CommonClientUtils;
import com.databasepreservation.visualization.client.main.BreadcrumbPanel;
import com.databasepreservation.visualization.shared.client.Tools.BreadcrumbManager;
import com.databasepreservation.visualization.shared.client.Tools.ViewerStringUtils;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * @author Bruno Ferreira <bferreira@keep.pt>
 */
public class SchemaCheckConstraintsPanel extends RightPanel {
  private static final ClientMessages messages = GWT.create(ClientMessages.class);
  private static Map<String, SchemaCheckConstraintsPanel> instances = new HashMap<>();

  public static SchemaCheckConstraintsPanel getInstance(ViewerDatabase database, String schemaUUID) {
    String separator = "/";
    String code = database.getUUID() + separator + schemaUUID;

    SchemaCheckConstraintsPanel instance = instances.get(code);
    if (instance == null) {
      instance = new SchemaCheckConstraintsPanel(database, schemaUUID);
      instances.put(code, instance);
    }
    return instance;
  }

  interface SchemaCheckConstraintsPanelUiBinder extends UiBinder<Widget, SchemaCheckConstraintsPanel> {
  }

  private static SchemaCheckConstraintsPanelUiBinder uiBinder = GWT.create(SchemaCheckConstraintsPanelUiBinder.class);

  private ViewerDatabase database;
  private ViewerSchema schema;

  @UiField
  FlowPanel contentItems;

  private SchemaCheckConstraintsPanel(ViewerDatabase viewerDatabase, final String schemaUUID) {
    database = viewerDatabase;
    schema = database.getMetadata().getSchema(schemaUUID);

    initWidget(uiBinder.createAndBindUi(this));
    init();
  }

  @Override
  public void handleBreadcrumb(BreadcrumbPanel breadcrumb) {
    BreadcrumbManager.updateBreadcrumb(
      breadcrumb,
      BreadcrumbManager.forSchemaCheckConstraints(database.getMetadata().getName(), database.getUUID(),
        schema.getName(), schema.getUUID()));
  }

  private void init() {
    CommonClientUtils.addSchemaInfoToFlowPanel(contentItems, schema);

    boolean atLeastOneConstraint = false;
    for (ViewerTable viewerTable : schema.getTables()) {
      if (!viewerTable.getCheckConstraints().isEmpty()) {
        atLeastOneConstraint = true;
        break;
      }
    }

    if (atLeastOneConstraint) {
      for (ViewerTable viewerTable : schema.getTables()) {
        if (!viewerTable.getCheckConstraints().isEmpty()) {
          contentItems.add(getBasicTablePanelForTableCheckConstraints(viewerTable));
        }
      }
    } else {
      Label noCheckConstraints = new Label(messages.constraints_thisSchemaDoesNotHaveAnyCheckConstraints());
      noCheckConstraints.addStyleName("strong");
      contentItems.add(noCheckConstraints);
    }
  }

  private BasicTablePanel<ViewerCheckConstraint> getBasicTablePanelForTableCheckConstraints(final ViewerTable table) {
    Label header = new Label(messages.constraints_constraintsInTable(table.getName()));
    header.addStyleName("h4");

    HTMLPanel info = new HTMLPanel("");

    return new BasicTablePanel<>(header, info, table.getCheckConstraints().iterator(),

    new BasicTablePanel.ColumnInfo<>(messages.name(), 15, new TextColumn<ViewerCheckConstraint>() {
      @Override
      public String getValue(ViewerCheckConstraint viewerRoutineParameter) {
        return viewerRoutineParameter.getName();
      }
    }),

    new BasicTablePanel.ColumnInfo<>(messages.constraints_condition(), 15, new TextColumn<ViewerCheckConstraint>() {
      @Override
      public String getValue(ViewerCheckConstraint viewerRoutineParameter) {
        if (ViewerStringUtils.isNotBlank(viewerRoutineParameter.getCondition())) {
          return viewerRoutineParameter.getCondition();
        } else {
          return "";
        }
      }
    }),

    new BasicTablePanel.ColumnInfo<>(messages.description(), 35, new TextColumn<ViewerCheckConstraint>() {
      @Override
      public String getValue(ViewerCheckConstraint viewerRoutineParameter) {
        if (ViewerStringUtils.isNotBlank(viewerRoutineParameter.getDescription())) {
          return viewerRoutineParameter.getDescription();
        } else {
          return "";
        }
      }
    })

    );
  }
}
