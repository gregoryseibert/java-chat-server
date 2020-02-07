package com.company;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathSolver {
    Map<String, String> variables = new HashMap<String, String>();

    public Double input(String input) {
        Matcher matcher = Pattern.compile("\\d\\s\\d").matcher(input);
        if(matcher.find()) {
            throw new RuntimeException();
        }
        matcher = Pattern.compile("[a-z][a-z]").matcher(input);
        if(matcher.find()) {
            throw new RuntimeException();
        }

        input = input.replaceAll("\\s", "");

        String nesting = extractNesting(input);
        if(nesting.length() > 0) {
            double nestingResult = input(nesting);
            input = input.replace("(" + nesting + ")", String.valueOf(nestingResult));

            return input(input);
        }

        List<String> numbers = extractNumbers(input);
        if(numbers.size() == 0) {
            return null;
        }
        if(numbers.size() == 1) {
            if(numbers.get(0).matches("[a-z]")) {
                String value = variables.get(numbers.get(0));
                if(value.length() > 0) {
                    return Double.valueOf(value);
                } else {
                    throw new RuntimeException();
                }
            }

            return Double.valueOf(numbers.get(0));
        }

        List<String> operators = extractOperators(input);
        if(operators.size() == 0) {
            return null;
        }
        if(operators.get(0).equals("=")) {
            if(operators.size() > 1) {
                double result = input(input.substring(2, input.length()));
                variables.put(numbers.get(0), String.valueOf(result));
                return result;
            } else {
                variables.put(numbers.get(0), numbers.get(1));
                return Double.valueOf(numbers.get(1));
            }
        } else if(operators.size() == 1) {
            double firstValue = 0;
            if(numbers.get(0).matches("[a-z]")) {
                firstValue = resolveVariable(numbers.get(0));
            } else {
                firstValue = Double.valueOf(numbers.get(0));
            }

            double secondValue = 0;
            if(numbers.get(1).matches("[a-z]")) {
                secondValue = resolveVariable(numbers.get(1));
            } else {
                secondValue = Double.valueOf(numbers.get(1));
            }

            return solveSimpleEquation(operators.get(0), firstValue, secondValue);
        }

        int i = 0;
        do {
            String operator = operators.get(i);

            if(operator.equals("*") || operator.equals("/") || operator.equals("%")) {
                double numberOne = Double.valueOf(numbers.get(i));
                double numberTwo = Double.valueOf(numbers.get(i + 1));
                double result = solveSimpleEquation(operator, numberOne, numberTwo);

                operators.remove(i);
                numbers.set(i, String.valueOf(result));
                numbers.remove(i + 1);
            } else {
                i++;
            }
        } while(i < operators.size());

        while(operators.size() != 0) {
            String operator = operators.get(0);
            double numberOne = Double.valueOf(numbers.get(0));
            double numberTwo = Double.valueOf(numbers.get(1));
            double result = solveSimpleEquation(operator, numberOne, numberTwo);

            operators.remove(0);
            numbers.set(0, String.valueOf(result));
            numbers.remove(1);
        }

        return Double.valueOf(numbers.get(0));
    }

    double resolveVariable(String var) {
        String value = variables.get(var);

        if(value == null || value.length() > 0) {
            return Double.valueOf(value);
        } else {
            throw new RuntimeException();
        }
    }

    double solveSimpleEquation(String operator, double numberOne, double numberTwo) {
        switch(operator) {
            case "+":
                return numberOne + numberTwo;
            case "-":
                return numberOne - numberTwo;
            case "*":
                return numberOne * numberTwo;
            case "/":
                return numberOne / numberTwo;
            case "%":
                return numberOne % numberTwo;
        }

        return 0;
    }

    String extractNesting(String expression) {
        int nestingStart = 0;

        for(int i = 0; i < expression.length(); i++) {
            if(expression.charAt(i) == '(') {
                nestingStart = i;
            } else if (expression.charAt(i) == ')') {
                return expression.substring(nestingStart + 1, i);
            }
        }

        return "";
    }

    List<String> extractOperators(String expression) {
        List<String> operators = matchAll("[\\w][\\/\\+\\-\\*\\=\\%]", expression);

        for(int i = 0; i < operators.size(); i++) {
            if(operators.get(i).length() > 1) {
                operators.set(i, operators.get(i).substring(1, 2));
            }
        }

        return operators;
    }

    List<String> extractNumbers(String expression) {
        List<String> numbers = matchAll("[\\*\\/\\+\\-]*([a-z])|(\\d+(\\.\\d+)?)", expression);

        if(numbers.size() == 1) {
            return numbers;
        }

        for(int i = 0; i < numbers.size(); i++) {
            if(numbers.get(i).length() >= 2) {
                char firstChar = numbers.get(i).charAt(0);

                if(firstChar == '+' || firstChar == '*' || firstChar == '/') {
                    numbers.set(i, numbers.get(i).substring(1));
                } else if(firstChar == '-' && numbers.get(i).charAt(1) == '-') {
                    numbers.set(i, numbers.get(i).substring(1));
                } else if(i > 0 && firstChar == '-' && Character.isDigit(numbers.get(i).charAt(1))) {
                    numbers.set(i, numbers.get(i).substring(1));
                }
            }
        }

        return numbers;
    }

    List<String> matchAll(String regex, String expression) {
        List<String> allMatches = new ArrayList<String>();
        Matcher matcher = Pattern.compile(regex).matcher(expression);

        while (matcher.find()) {
            allMatches.add(matcher.group());
        }

        return allMatches;
    }
}
