package app;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;

import app.context.ApplicationContext;
import app.context.ApplicationParameters;
import app.utils.FileUtils;
import app.utils.GeometryUtils;
import app.utils.JSONUtils;
import dto.Edge;
import dto.EdgeEndpoint;
import dto.Graph;
import dto.GraphElement;
import dto.Point;
import dto.Vertex;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class GraphForm {
    private static final int VERTEX_WIDTH = 100;
    private static final int VERTEX_HEIGHT = 50;
    private static final int VERTEX_TEXT_PADDING = 50;
    private static final int HOVER_LINE_WIDTH = 10;

    private static final Color COLOR_VERTEX_TEXT = Color.BLUE;
    private static final Color COLOR_VERTEX_BORDER = Color.BLUE;
    private static final Color COLOR_EDGE = Color.BLUE;
    private static final Color COLOR_HOVERLINE = Color.GREEN;
    private static final Color COLOR_EDGE_NEW = Color.GREEN;

    private ApplicationContext applicationContext;

    // General
    @FXML
    private Pane imagePane;
    @FXML
    private ImageView imageView;

    private File graphFile;
    private Graph graph;

    private GraphElement selectedElement;
    private double lastPosX;
    private double lastPosY;

    private double scale = 1;

    private Line hoverLine;
    private EdgeEndpoint edgeStart;
    private Line newEdgeLine;

    public GraphForm() {
        hoverLine = new Line();
        hoverLine.setFill(null);
        hoverLine.setStroke(COLOR_HOVERLINE);
        hoverLine.setStrokeWidth(HOVER_LINE_WIDTH / 2);

        newEdgeLine = new Line();
        newEdgeLine.setFill(null);
        newEdgeLine.setStroke(COLOR_EDGE_NEW);
        newEdgeLine.setStrokeWidth(HOVER_LINE_WIDTH / 2);
    }

    public Parent init(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;

        String graphFileName = applicationContext.getParameterValue(ApplicationParameters.GraphFileName);
        graphFile = new File(graphFileName);

        Parent root = FileUtils.loadFXML(this);

        imagePane.setOnMouseMoved(e -> handleMouseMoved(e));
        imagePane.setOnMousePressed(e -> handleMousePressed(e));
        imagePane.setOnMouseDragged(e -> handleMouseDrag(e));

        imagePane.setOnKeyPressed(e -> handleKeyPressed(e));

        restoreGraph();

        return root;
    }

    private void restoreGraph() {
        try {
            TypeReference<Graph> typeReference = new TypeReference<Graph>() {
            };
            graph = JSONUtils.loadFromDisk(graphFile, typeReference);
        } catch (IOException e) {
            e.printStackTrace();
            graph = new Graph();
        }

        renderGraph();
    }

    private void handleMouseMoved(MouseEvent e) {
        double posOnImageX = getImageCoordinateX(e.getSceneX());
        double posOnImageY = getImageCoordinateY(e.getSceneY());

        Vertex vertex = findVertex(posOnImageX, posOnImageY);
        updateVertexHoverLine(vertex, posOnImageX, posOnImageY);

        if (vertex == null) {
            Edge edge = findEdge(posOnImageX, posOnImageY);
            updateEdgeHoverLine(edge, posOnImageX, posOnImageY);
        }

        updateNewEdgeLine(posOnImageX, posOnImageY);
    }

    private void updateVertexHoverLine(Vertex vertex, double posOnImageX, double posOnImageY) {
        if (vertex == null) {
            hoverLine.setOpacity(0);
            return;
        }

        double vertexLeftX = vertex.getPosition().x;
        double vertexRightX = vertexLeftX + vertex.getSize().x;
        double vertexTopY = vertex.getPosition().y;
        double vertexBottomY = vertexTopY + vertex.getSize().y;

        double startX = 0, endX = 0, startY = 0, endY = 0, opacity = 0;

        // Left Edge
        if (posOnImageX >= vertexLeftX - HOVER_LINE_WIDTH && posOnImageX <= vertexLeftX + HOVER_LINE_WIDTH) {
            startX = endX = vertexLeftX;
            startY = vertexTopY;
            endY = vertexBottomY;
            opacity = 1;
        }

        // Top Edge
        if (posOnImageY >= vertexTopY - HOVER_LINE_WIDTH && posOnImageY <= vertexTopY + HOVER_LINE_WIDTH) {
            startX = vertexLeftX;
            endX = vertexRightX;
            startY = endY = vertexTopY;
            opacity = 1;
        }

        // Right Edge
        if (posOnImageX >= vertexRightX - HOVER_LINE_WIDTH && posOnImageX <= vertexRightX + HOVER_LINE_WIDTH) {
            startX = endX = vertexRightX;
            startY = vertexTopY;
            endY = vertexBottomY;
            opacity = 1;
        }

        // Bottom Edge
        if (posOnImageY >= vertexBottomY - HOVER_LINE_WIDTH && posOnImageY <= vertexBottomY + HOVER_LINE_WIDTH) {
            startX = vertexLeftX;
            endX = vertexRightX;
            startY = endY = vertexBottomY;
            opacity = 1;
        }

        hoverLine.setOpacity(opacity);
        hoverLine.setStartX(startX);
        hoverLine.setStartY(startY);
        hoverLine.setEndX(endX);
        hoverLine.setEndY(endY);
    }

    private void updateEdgeHoverLine(Edge edge, double posOnImageX, double posOnImageY) {
        if (edge == null) {
            hoverLine.setOpacity(0);
            return;
        }

        hoverLine.setOpacity(1);

        Point lineStart = Edge.getAbsolutePosition(edge.getEdgeStart());
        Point lineEnd = Edge.getAbsolutePosition(edge.getEdgeEnd());

        hoverLine.setStartX(lineStart.x);
        hoverLine.setStartY(lineStart.y);
        hoverLine.setEndX(lineEnd.x);
        hoverLine.setEndY(lineEnd.y);
    }

    private void updateNewEdgeLine(double posOnImageX, double posOnImageY) {
        if (edgeStart == null) {
            newEdgeLine.setOpacity(0);
            return;
        }

        Vertex vertex = edgeStart.getVertex();
        Point vertexPosition = vertex.getPosition();
        Point startPoint = edgeStart.getPoint();

        newEdgeLine.setOpacity(1);

        double startX = vertexPosition.x + startPoint.x;
        double startY = vertexPosition.y + startPoint.y;

        newEdgeLine.setStartX(startX);
        newEdgeLine.setStartY(startY);
        newEdgeLine.setEndX(posOnImageX);
        newEdgeLine.setEndY(posOnImageY);
    }

    private void handleMousePressed(MouseEvent e) {
        if (e.isPrimaryButtonDown()) {
            selectGraphElement(e.getSceneX(), e.getSceneY());
            if (e.getClickCount() == 2) {
                editSelectedVertex();
            }
        }

        saveLastMousePosition(e.getSceneX(), e.getSceneY());
    }

    private void selectGraphElement(double x, double y) {
        double posOnImageX = getImageCoordinateX(x);
        double posOnImageY = getImageCoordinateY(y);

        Vertex vertex = findVertex(posOnImageX, posOnImageY);
        Edge edge = findEdge(posOnImageX, posOnImageY);
        if (vertex != null) {
            selectedElement = vertex;
            checkAddEdge(vertex, posOnImageX, posOnImageY);
        } else if (edge != null) {
            selectedElement = edge;
        } else {
            addVertex(posOnImageX, posOnImageY);
        }

        renderGraph();
    }

    private void addVertex(double x, double y) {
        Vertex vertex;
        vertex = new Vertex();
        vertex.setPosition(x, y);
        vertex.setSize(VERTEX_WIDTH, VERTEX_HEIGHT);
        graph.addVertex(vertex);

        renderGraph();
    }

    private void saveLastMousePosition(double x, double y) {
        lastPosX = x;
        lastPosY = y;
    }

    private void handleMouseDrag(MouseEvent e) {
        double currentPosX = e.getSceneX();
        double currentPosY = e.getSceneY();
        if (e.isPrimaryButtonDown()) {
            if (selectedElement != null) {
                if (selectedElement instanceof Vertex) {
                    handleVertexDrag(currentPosX, currentPosY);
                } else if (selectedElement instanceof Edge) {
                    // TODO handleEdgeDrag
                }
            }
        } else if (e.isSecondaryButtonDown()) {
            handleImageDrag(currentPosX, currentPosY);
        }

        saveLastMousePosition(e.getSceneX(), e.getSceneY());
    }

    private void handleVertexDrag(double currentPosX, double currentPosY) {
        if (selectedElement == null || !(selectedElement instanceof Vertex)) {
            return;
        }

        double deltaX = currentPosX - lastPosX;
        double deltaY = currentPosY - lastPosY;

        Vertex selectedVertex = (Vertex) selectedElement;
        Point vertexOldPosition = selectedVertex.getPosition();
        selectedVertex.setPosition(vertexOldPosition.x + deltaX, vertexOldPosition.y + deltaY);

        renderGraph();
    }

    private void handleImageDrag(double currentPosX, double currentPosY) {
        double deltaX = currentPosX - lastPosX;
        double deltaY = currentPosY - lastPosY;

        imagePane.setTranslateX(imagePane.getTranslateX() + deltaX);
        imagePane.setTranslateY(imagePane.getTranslateY() + deltaY);
    }

    private double getImageCoordinateX(double screenCoordinate) {
        double sceneOffset = imagePane.getBoundsInParent().getMinX();
        double rectangleOffset = imagePane.getBoundsInLocal().getMinX();
        double result = screenCoordinate / scale - sceneOffset / scale + rectangleOffset;
        return result;
    }

    private double getImageCoordinateY(double screenCoordinate) {
        double sceneOffset = imagePane.getBoundsInParent().getMinY();
        double rectangleOffset = imagePane.getBoundsInLocal().getMinY();
        double result = screenCoordinate / scale - sceneOffset / scale + rectangleOffset;
        return result;
    }

    private void handleKeyPressed(KeyEvent e) {
        switch (e.getCode()) {
        case DELETE:
            deleteSelectedElement();
            break;
        case ENTER:
            editSelectedVertex();
            break;
        case ESCAPE:
            cancelAddEdge();
            break;
        default:
            break;
        }

        if (e.isControlDown()) {
            switch (e.getCode()) {
            case S:
                saveGraph();
                break;
            default:
                break;
            }
        }
    }

    private void deleteSelectedElement() {
        if (selectedElement == null) {
            return;
        }

        if (selectedElement instanceof Vertex) {
            deleteVertex((Vertex) selectedElement);
        } else if (selectedElement instanceof Edge) {
            deleteEdge((Edge) selectedElement);
        }
    }

    private void deleteVertex(Vertex vertex) {
        graph.removeVertex(vertex);
        renderGraph();
    }

    private void deleteEdge(Edge edge) {
        graph.removeEdge(edge);
        renderGraph();
    }

    private void cancelAddEdge() {
        if (edgeStart != null) {
            edgeStart = null;
        }

        updateNewEdgeLine(lastPosX, lastPosY);
    }

    private Vertex findVertex(double x, double y) {
        for (Vertex vertex : graph.getVertices()) {
            Point position = vertex.getPosition();
            Point size = vertex.getSize();

            boolean matches = x >= position.x && x <= position.x + size.x;
            matches &= y >= position.y && y <= position.y + size.y;

            if (matches) {
                return vertex;
            }
        }
        return null;
    }

    private Edge findEdge(double posOnImageX, double posOnImageY) {
        for (Edge edge : graph.getEdges()) {
            Point lineStart = Edge.getAbsolutePosition(edge.getEdgeStart());
            Point lineEnd = Edge.getAbsolutePosition(edge.getEdgeEnd());
            double distance = GeometryUtils.distanceBetweenPointAndLine(new Point(posOnImageX, posOnImageY), lineStart, lineEnd);
            if (distance <= HOVER_LINE_WIDTH) {
                return edge;
            }
        }
        return null;
    }

    private void checkAddEdge(Vertex vertex, double x, double y) {
        double vertexLeftX = vertex.getPosition().x;
        double vertexRightX = vertexLeftX + vertex.getSize().x;
        double vertexTopY = vertex.getPosition().y;
        double vertexBottomY = vertexTopY + vertex.getSize().y;

        //edgeStart = null;
        Point point = null;
        if (vertex != null) {
            // Left Edge
            if (x >= vertexLeftX - HOVER_LINE_WIDTH && x <= vertexLeftX + HOVER_LINE_WIDTH) {
                point = new Point(0, y - vertexTopY);
            }

            // Top Edge
            if (y >= vertexTopY - HOVER_LINE_WIDTH && y <= vertexTopY + HOVER_LINE_WIDTH) {
                point = new Point(x - vertexLeftX, 0);
            }

            // Right Edge
            if (x >= vertexRightX - HOVER_LINE_WIDTH && x <= vertexRightX + HOVER_LINE_WIDTH) {
                point = new Point(vertexRightX - vertexLeftX, y - vertexTopY);
            }

            // Bottom Edge
            if (y >= vertexBottomY - HOVER_LINE_WIDTH && y <= vertexBottomY + HOVER_LINE_WIDTH) {
                point = new Point(x - vertexLeftX, vertexBottomY - vertexTopY);
            }

            if (edgeStart == null) {
                if (point != null) {
                    edgeStart = new EdgeEndpoint(point, vertex);
                }
            } else {
                if (edgeStart.getVertex() != vertex) {
                    addEdge(edgeStart, new EdgeEndpoint(point, vertex));
                }
                edgeStart = null;
            }
        }
    }

    private void addEdge(EdgeEndpoint edgeStart, EdgeEndpoint edgeEnd) {
        EdgeEndpoint relativeEdgeStart = sceneToRelativeEndpoint(edgeStart);
        EdgeEndpoint relativeEdgeEnd = sceneToRelativeEndpoint(edgeEnd);
        Edge edge = new Edge(relativeEdgeStart, relativeEdgeEnd);
        graph.addEdge(edge);
        renderGraph();
    }

    private EdgeEndpoint sceneToRelativeEndpoint(EdgeEndpoint sceneEndpoint) {
        if (sceneEndpoint == null) {
            return null;
        }

        Vertex vertex = sceneEndpoint.getVertex();

        EdgeEndpoint relativeEndpoint = new EdgeEndpoint();
        relativeEndpoint.setVertex(vertex);

        double relativePositionX = sceneEndpoint.getPoint().x / vertex.getSize().x;
        double relativePositionY = sceneEndpoint.getPoint().y / vertex.getSize().y;
        Point relativePosition = new Point(relativePositionX, relativePositionY);
        relativeEndpoint.setPoint(relativePosition);

        return relativeEndpoint;
    }

    private void renderGraph() {
        imagePane.getChildren().clear();
        hoverLine.setOpacity(0);

        imagePane.getChildren().add(hoverLine);
        imagePane.getChildren().add(newEdgeLine);

        for (Vertex vertex : graph.getVertices()) {
            Color color = vertex == selectedElement ? Color.RED : COLOR_VERTEX_BORDER;
            Rectangle rectange = createVertexRectangle(vertex, color);
            imagePane.getChildren().add(rectange);
            Text text = createVertexText(vertex);
            imagePane.getChildren().add(text);
        }

        for (Edge edge : graph.getEdges()) {
            Color color = edge == selectedElement ? Color.RED : COLOR_EDGE;
            Line line = createEdgeLine(edge, color);
            imagePane.getChildren().add(line);
        }
    }

    private Rectangle createVertexRectangle(Vertex vertex, Color color) {
        Point position = vertex.getPosition();
        Point size = vertex.getSize();

        Rectangle rectange = new Rectangle(size.x, size.y, null);
        rectange.setStroke(color);
        rectange.setX(position.x);
        rectange.setY(position.y);

        return rectange;
    }

    private Text createVertexText(Vertex vertex) {
        Text text = new Text(vertex.getText());
        text.setOnMouseDragged(e -> handleMouseDrag(e));
        text.setFill(COLOR_VERTEX_TEXT);

        Bounds bounds = text.getLayoutBounds();
        double x = vertex.getPosition().x + (vertex.getSize().x - bounds.getWidth()) / 2;
        double y = vertex.getPosition().y + (vertex.getSize().y - bounds.getHeight() + 24) / 2;
        text.setX(x);
        text.setY(y);

        return text;
    }

    private Line createEdgeLine(Edge edge, Color color) {
        Line line = new Line();
        line.setStroke(color);

        Point edgeStartPosition = calculateEdgeEndpointPosition(edge.getEdgeStart());
        Point edgeEndPosition = calculateEdgeEndpointPosition(edge.getEdgeEnd());

        line.setStartX(edgeStartPosition.x);
        line.setStartY(edgeStartPosition.y);
        line.setEndX(edgeEndPosition.x);
        line.setEndY(edgeEndPosition.y);

        return line;
    }

    private Point calculateEdgeEndpointPosition(EdgeEndpoint edgeEndpoint) {
        Point startVertexPosition = edgeEndpoint.getVertex().getPosition();
        Point startVertexSize = edgeEndpoint.getVertex().getSize();
        Point edgeStartPosition = edgeEndpoint.getPoint();

        double x = startVertexPosition.x + edgeStartPosition.x * startVertexSize.x;
        double y = startVertexPosition.y + edgeStartPosition.y * startVertexSize.y;

        Point position = new Point (x, y);
        return position;
    }

    private void editSelectedVertex() {
        if (selectedElement == null || !(selectedElement instanceof Vertex)) {
            return;
        }

        Vertex vertex = (Vertex) selectedElement;

        try {
            Stage stage = applicationContext.getStage();
            VertexEditForm vertexEditForm = new VertexEditForm(stage);
            vertexEditForm.setVertex(vertex);
            vertexEditForm.showAndWait();

            updateVertexSize(vertex);
            renderGraph();
        } catch (IOException e) {
            applicationContext.showError("Could not Edit Vertex", e);
        }
    }

    private void updateVertexSize(Vertex vertex) {
        Text text = new Text(vertex.getText());

        Bounds textBounds = text.getBoundsInLocal();
        double width = Math.max(textBounds.getWidth() + VERTEX_TEXT_PADDING, VERTEX_WIDTH);
        double height = Math.max(textBounds.getHeight() + VERTEX_TEXT_PADDING, VERTEX_HEIGHT);
        vertex.setSize(width, height);
    }

    private void saveGraph() {
        try {
            JSONUtils.saveToDisk(graph, graphFile);
        } catch (IOException e) {
            applicationContext.showError("Error by Saving Graph", e);
        }
    }
}
