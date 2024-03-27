package process.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import app.utils.FileUtils;
import app.utils.StringUtils;
import process.parser.dto.Action;
import process.parser.dto.Chain;
import process.parser.dto.Command;
import process.parser.dto.Command.Condition;
import process.parser.dto.Command.Positions;
import process.parser.dto.Variable;
import process.parser.dto.html.HtmlElement;
import process.parser.dto.html.TagElement;
import process.parser.dto.html.TextElement;

public class Transformator {
    private static final String LOG_FILENAME = "transform.log";
    private HtmlRenderer renderer;

    public Transformator() {
        renderer = new HtmlRenderer();
    }

    public List<HtmlElement> transform(Chain<HtmlElement> chain, List<Command> commands, Map<String, Variable> variables) throws IOException {
        FileUtils.createFile(LOG_FILENAME, true);
        FileUtils.printLine(LOG_FILENAME, "Starting to transform");

        List<HtmlElement> transformationResult = new ArrayList<>();

        List<HtmlElement> currentResultContent = transformationResult;
        TagElement currentResultParentTag = null;
        TagElement currentResultTag = null;

        Chain<HtmlElement> currentChain = chain;

        main: while (currentChain != null) {
            HtmlElement currentSourceElement = currentChain.getValue();
            String message = "Processing element: " + renderer.render(Collections.singletonList(currentSourceElement));
            FileUtils.printLine(LOG_FILENAME, message);

            if (!(currentSourceElement instanceof TagElement)) {
                // Removing current element from the Chain
                Chain<HtmlElement> previousChain = currentChain.getPreviousChain();
                currentChain = currentChain.getNextChain();
                if (currentChain != null) {
                    currentChain.linkPrevious(previousChain);
                }
                continue main;
            }

            TagElement currentSourceTag = (TagElement) currentSourceElement;

            commands: for (Command command : commands) {
                // Null Commands can be the result of Empty Lines or Comments
                if (command == null) {
                    continue;
                }

                // Checking that Command Matches current Element
                if (!isElementMatches(currentChain, command)) {
                    continue;
                }

                FileUtils.printLine(LOG_FILENAME, "\tApplying command: " + command);

                actions: for (Action action : command.getActions()) {
                    switch (action.getName()) {
                    case "add":
                        currentSourceTag.setParentContent(currentResultContent);
                        currentSourceTag.setParentTag(currentResultParentTag);
                        currentResultContent.add(currentSourceTag);

                        currentResultTag = currentSourceTag;

                        currentChain = currentChain.getNextChain();
                        continue main;

                    case "addAsParent":
                        currentSourceTag.setParentTag(currentResultParentTag);
                        currentSourceTag.setParentContent(currentResultContent);
                        currentResultContent.add(currentSourceTag);

                        currentResultParentTag = currentSourceTag;
                        currentResultTag = currentSourceTag;
                        currentResultContent = currentResultTag.getContent();

                        currentChain = currentChain.getNextChain();
                        continue main;

                    case "delete":
                        currentChain = currentChain.getNextChain();
                        continue main;

                    case "demoteParent":
                        if (currentResultParentTag == null) {
                            String tagText = renderer.render(Collections.singletonList(currentSourceTag));
                            throw new IllegalStateException("Cannot demote parent: " + tagText);
                        }

                        currentResultContent = currentResultParentTag.getParentContent();
                        currentResultParentTag = currentResultParentTag.getParentTag();
                        break;

                    case "createTag":
                        String arguments = action.getArguments();

                        TagElement newTag = new TagElement();
                        newTag.setName(arguments);
                        newTag.setParentContent(currentResultContent);
                        newTag.setParentTag(currentResultParentTag);
                        currentResultContent.add(newTag);
                        break;

                    case "createParentTag":
                        arguments = action.getArguments();

                        newTag = new TagElement();
                        newTag.setName(arguments);
                        newTag.setParentContent(currentResultContent);
                        newTag.setParentTag(currentResultParentTag);
                        currentResultContent.add(newTag);

                        currentResultParentTag = newTag;
                        currentResultTag = newTag;
                        currentResultContent = currentResultTag.getContent();
                        break;

                    case "closeParentIfOpen":
                        arguments = action.getArguments();
                        TagElement tagToClose = findParentTagRecursively(currentResultTag, arguments);
                        if (tagToClose != null) {
                            currentResultTag = tagToClose.getParentTag();
                            currentResultParentTag = currentResultTag == null ? null : currentResultTag.getParentTag();
                            currentResultContent = tagToClose.getParentContent();
                        }
                        break;

                    case "changeTagTo":
                        arguments = action.getArguments();
                        currentSourceTag.setName(arguments);
                        break;

                    case "clearAttributes":
                        currentSourceTag.getAttributes().clear();
                        break;

                    case "concatToPrevious":
                        List<HtmlElement> currentSourceContent = currentSourceTag.getContent();
                        if (currentResultTag == null) {
                            throw new IllegalStateException("Cannot concat to previous, because previous is null");
                        }
                        currentResultTag.getContent().addAll(currentSourceContent);

                        currentChain = currentChain.getNextChain();
                        continue main;

                    case "deleteNestedTag":
                        arguments = action.getArguments();
                        Iterator<HtmlElement> iterator = currentSourceTag.getContent().iterator();
                        while (iterator.hasNext()) {
                            HtmlElement element = iterator.next();
                            if (element instanceof TagElement && ((TagElement) element).getName().equals(arguments)) {
                                iterator.remove();
                            }
                        }
                        break;

                    case "insertAtStart":
                        arguments = action.getArguments();
                        TextElement textElement = new TextElement();
                        textElement.setValue(arguments);
                        currentSourceTag.getContent().add(0, textElement);
                        break;

                    case "insertAtEnd":
                        arguments = action.getArguments();
                        textElement = new TextElement();
                        textElement.setValue(arguments);
                        currentSourceTag.getContent().add(textElement);
                        break;

                    case "replaceNestedTagWithTag":
                        arguments = action.getArguments();
                        String[] argumentsParts = arguments.split(",");
                        if (argumentsParts.length < 2) {
                            throw new IllegalArgumentException(
                                    "Invalid arguments \"" + arguments + "\" for action: " + action.getName());
                        }

                        TagElement replacementTag = new TagElement();
                        replacementTag.setName(argumentsParts[1]);
                        List<HtmlElement> content = currentSourceTag.getContent();
                        for (int i = 0; i < content.size(); i++) {
                            HtmlElement element = content.get(i);
                            if (element instanceof TagElement && ((TagElement) element).getName().equals(argumentsParts[0])) {
                                content.set(i, replacementTag);
                            }
                        }
                        break;

                    case "replaceNestedTagWithString":
                        arguments = action.getArguments();
                        argumentsParts = arguments.split(",");
                        if (argumentsParts.length < 2) {
                            throw new IllegalArgumentException(
                                    "Invalid arguments \"" + arguments + "\" for action: " + action.getName());
                        }

                        TextElement replacementText = new TextElement();
                        replacementText.setValue(argumentsParts[1]);
                        content = currentSourceTag.getContent();
                        for (int i = 0; i < content.size(); i++) {
                            HtmlElement element = content.get(i);
                            if (element instanceof TagElement && ((TagElement) element).getName().equals(argumentsParts[0])) {
                                content.set(i, replacementText);
                            }
                        }
                        break;

                        // Replace Substrings (need to define variable and put as first argument,
                        // like:
                        // define table = StringArray["a", "b"]
                        // curr{span}->replaceSubstrings(table)
                        case "replaceSubstrings":
                            arguments = action.getArguments();
                            Variable variable = variables.get(arguments);
                            if (variable == null) {
                                throw new IllegalArgumentException("Variable \"" + arguments + "\" is not defined");
                            }
                            String[] replacementTable = (String[]) variable.getValue();
                            replaceSubstringsRecursively(currentSourceTag.getContent(), replacementTable);
                         break;

                        case "toLowerCase":
                            processContentRecursively(currentSourceTag.getContent(), value -> value.toLowerCase());
                            break;

                        case "toUpperCase":
                            processContentRecursively(currentSourceTag.getContent(), value -> value.toUpperCase());
                            break;

                        case "toCamelCase":
                            processContentRecursively(currentSourceTag.getContent(), value -> StringUtils.toCamelCase(value.toUpperCase()));
                            break;

                        case "trim":
                            processContentRecursively(currentSourceTag.getContent(), value -> value.trim());
                            break;

                    default:
                        throw new IllegalArgumentException("Unknown command action: " + action.getName());
                    }
                }
            }

            currentSourceTag.setParentContent(currentResultContent);
            currentSourceTag.setParentTag(currentResultParentTag);
            currentResultContent.add(currentSourceTag);

            currentResultTag = currentSourceTag;

            currentChain = currentChain.getNextChain();
        }

        return transformationResult;
    }

    protected boolean isElementMatches(Chain<HtmlElement> chain, Command command) {
        boolean matches = true;

        if (command.getCondition(Positions.Previous) != null) {
            matches &= isElementMatches(chain.getPreviousChain(), command.getCondition(Positions.Previous));
        }

        if (command.getCondition(Positions.Current) != null) {
            matches &= isElementMatches(chain, command.getCondition(Positions.Current));
        }

        if (command.getCondition(Positions.Next) != null) {
            matches &= isElementMatches(chain.getNextChain(), command.getCondition(Positions.Next));
        }

        return matches;
    }

    private boolean isElementMatches(Chain<HtmlElement> chain, Condition condition) {
        if (chain == null || chain.getValue() == null || !(chain.getValue() instanceof TagElement)) {
            return false;
        }

        boolean matches = true;
        TagElement element = (TagElement) chain.getValue();

        if (!condition.getTagName().equals(element.getName())) {
            matches = false;
        }

        String conditionAttributes = condition.getAttributes().toString();
        String elementAttributes = element.getAttributes().toString();
        matches &= conditionAttributes.equals(elementAttributes);

        return matches;
    }

    private TagElement findParentTagRecursively(TagElement tagElement, String tagNameToFind) {
        if (tagElement.getName().equals(tagNameToFind)) {
            return tagElement;
        }

        TagElement parentTag = tagElement.getParentTag();
        if (parentTag != null) {
            return findParentTagRecursively(parentTag, tagNameToFind);
        }

        return null;
    }

    private void replaceSubstringsRecursively(List<HtmlElement> content, String[] replacementTable) {
        content.forEach(element -> {
            if (element instanceof TextElement) {
                String value = ((TextElement) element).getValue();
                value = replaceSubstrings(value, replacementTable);
                ((TextElement) element).setValue(value);
            } else if (element instanceof TagElement) {
                replaceSubstringsRecursively(((TagElement) element).getContent(), replacementTable);
            }
        });

    }

    private String replaceSubstrings(String currentContent, String[] replacementTable) {
        String replacedString = currentContent;
        for (int i = 0; i < replacementTable.length; i += 2) {
            String regex = replacementTable[i];
            String replacement = replacementTable[i + 1];
            replacedString = replacedString.replaceAll(regex, replacement);
        }
        return replacedString;
    }

    private void processContentRecursively(List<HtmlElement> content, Function<String, String> function) {
        content.forEach(element -> {
            if (element instanceof TextElement) {
                String value = ((TextElement) element).getValue();
                value = function.apply(value);
                ((TextElement) element).setValue(value);
            } else if (element instanceof TagElement) {
                processContentRecursively(((TagElement) element).getContent(), function);
            }
        });
    }
}
