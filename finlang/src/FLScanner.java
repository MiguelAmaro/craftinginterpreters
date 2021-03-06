package com.finlang.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.finlang.lang.FLTokenType.*; 

class FLScanner
{
    private final String source;
    private final List<FLToken> tokens = new ArrayList<>();
    private int start   = 0;
    private int current = 0;
    private int line    = 1;
    
    private static final Map<String, FLTokenType> keywords;
    static
    {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("struct", STRUCT);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("null",   NULL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        
        keywords.put("#ifndef", PP_IFNDEF);
        keywords.put("#define", PP_DEF);
        keywords.put("#if"    , PP_IF);
        keywords.put("#else"  , PP_ELSE);
        keywords.put("#endif" , PP_ENDIF);
        
        
        keywords.put("u8",     NATIVE_U8);
        keywords.put("u16",    NATIVE_U16);
        keywords.put("u32",    NATIVE_U32);
        keywords.put("u64",    NATIVE_U64);
        
        keywords.put("s8",     NATIVE_S8);
        keywords.put("s16",    NATIVE_S16);
        keywords.put("s32",    NATIVE_S32);
        keywords.put("s64",    NATIVE_S64);
        keywords.put("while",  WHILE);
    }
    
    FLScanner(String source)
    {
        this.source = source;
    }
    
    List<FLToken> scanTokens()
    {
        while (!isAtEnd())
        {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }
        
        tokens.add(new FLToken(EOF, "", null, line));
        return tokens;
    }
    
    private boolean isAtEnd()
    {
        return current >= source.length();
    }
    
    private void scanToken()
    {
        char c = advance();
        switch (c)
        {
            case '(': addToken(PAREN_LEFT ); break;
            case ')': addToken(PAREN_RIGHT); break;
            case '{': addToken(BRACE_LEFT ); break;
            case '}': addToken(BRACE_RIGHT); break;
            case ',': addToken(COMMA      ); break;
            case '.': addToken(DOT        ); break;
            case ':': addToken(COLON      ); break; // TODO(MIGUEL): use this for declaration
            case '-': addToken(MINUS      ); break;
            case '+': addToken(PLUS       ); break;
            case ';': addToken(SEMICOLON  ); break;
            case '*': addToken(STAR       ); break;
            case '!':
            {
                addToken(match('=') ? NOT_EQUAL : NOT);
            } break;
            case '=':
            {
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            } break;
            case '<':
            {
                addToken(match('=') ? LESS_EQUAL : LESS);
            } break;
            case '>':
            {
                addToken(match('=') ? GREATER_EQUAL : GREATER);
            } break;
            case '/':
            {
                if (match('/'))
                {
                    // NOTE(MIGUEL): throws away comment
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) advance();
                }
                else if(match('*'))
                {
                    
                    while ((!isAtEnd())) 
                    { 
                        advance();
                        
                        if(match('*') && peek() == '/')
                        {
                            advance();
                            break;
                        }
                        
                    }
                }
                else
                {
                    addToken(SLASH);
                }
            } break;
            
            case ' ':
            case '\r':
            case '\t':
            // Ignore whitespace.
            break;
            
            case '\n':
            line++;
            break;
            
            case '"': string(); break;
            
            default :
            {
                
                if (isDigit(c))
                {
                    if(c == '0')
                    {
                        // NOTE(MIGUEL): should this be native???
                        if(false)
                        {}
                        else if(match('x') || match('X'))
                        {
                            hex();
                        }
                        else if(match('b') || match('B'))
                        {
                            bin();
                        }
                        else
                        {
                            number();
                        }
                    }
                    else
                    {
                        number();
                    }
                }
                else if (isAlpha(c))
                {
                    // NOTE(MIGUEL): preprocessor token created here
                    identifier();
                }
                else
                {
                    Finlang.error(line, "Unexpected character.");
                }
            }break;
        }
    }
    
    private void identifier()
    {
        while (isAlphaNumeric(peek())) advance();
        
        String      text = source.substring(start, current);
        FLTokenType type = keywords.get(text);
        
        if (type == null) type = IDENTIFIER;
        
        addToken(type);
        
        return;
    }
    
    private boolean isAlpha(char c)
    {
        return ((c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c == '_'));
    }
    
    private boolean isAlphaNumeric(char c)
    {
        return isAlpha(c) || isDigit(c);
    }
    
    private void number()
    {
        while (isDigit(peek())) advance();
        
        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext()))
        {
            // Consume the "."
            advance();
            
            while (isDigit(peek())) advance();
        }
        
        // NOTE(MIGUEL): how do this play into supporting native types
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }
    
    private void hex()
    {
        while (isHex(peek())) advance();
        
        String num = source.substring(start + 2, current);
        
        if((num.charAt(0) > '7') && (num.length() >= 8))
        {
            addToken(NUMBER, Long.parseLong(num, 16));
        }
        else
        {
            addToken(NUMBER, Integer.parseInt(num, 16));
        }
    }
    
    
    private void bin()
    {
        while (isBin(peek())) advance();
        
        
        // NOTE(MIGUEL): how do this play into supporting native types
        addToken(NUMBER, Integer.parseInt(source.substring(start + 2, current), 2));
    }
    
    
    private void string()
    {
        while (peek() != '"' && !isAtEnd())
        {
            if (peek() == '\n') line++;
            advance();
        }
        
        if (isAtEnd())
        {
            Finlang.error(line, "Unterminated string.");
            return;
        }
        
        // The closing ".
        advance();
        
        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
        
        return;
    }
    
    private boolean match(char expected)
    {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        
        current++;
        return true;
    }
    
    private char peek()
    {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }
    
    private char peekNext()
    {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    } 
    
    private boolean isDigit(char c)
    {
        return c >= '0' && c <= '9';
    } 
    
    private boolean isHex(char c)
    {
        return ((c >= '0' && c <= '9') ||
                (c >= 'A' && c <= 'F') ||
                (c >= 'a' && c <= 'f'));
    } 
    
    
    private boolean isBin(char c)
    {
        return c == '0' || c == '1';
    } 
    
    
    private char advance()
    {
        return source.charAt(current++);
    }
    
    private void addToken(FLTokenType type)
    {
        addToken(type, null);
        
        return;
    }
    
    private void addToken(FLTokenType type, Object literal)
    {
        String text = source.substring(start, current);
        tokens.add(new FLToken(type, text, literal, line));
        
        return;
    }
}


