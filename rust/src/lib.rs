#![allow(unused_variables)]
#![allow(dead_code)]

use crate::ParseState::{Digit, Str, Bottom, Value, Arr, Map, MapKey, MapColon};

type Vu8 = Vec<u8>;

#[derive(Debug, Copy, Clone)]
enum ParseState {
    Bottom,
    Value,
    Digit { data_start: usize },
    Str { data_start: usize },
    Arr { elem_count: u16, meta_start: usize, meta_len_index: usize, data_len_index: usize },
    Map { elem_count: u16, meta_start: usize, meta_len_index: usize, data_len_index: usize },
    MapKey { data_start: usize },
    MapColon,
    Terminal,
    Empty,
}

// TODO the 0 byte type should be for unknown char
pub const BT_TERMINAL: u8 = 0;
pub const BT_BLANK: u8 = 1;
pub const BT_DIGIT: u8 = 2;
pub const BT_ALPHA: u8 = 3;
pub const BT_DOUBLE_QUOTE: u8 = 4;
pub const BT_OPEN_ARR: u8 = 5;
pub const BT_CLOSE_ARR: u8 = 6;
pub const BT_OPEN_MAP: u8 = 7;
pub const BT_CLOSE_MAP: u8 = 8;
pub const BT_COMMA: u8 = 9;
pub const BT_COLON: u8 = 10;

const TAG_STR: u8 = 1;
const TAG_INT: u8 = 2;
const TAG_MAP: u8 = 3;
const TAG_ARR: u8 = 4;
const M_TAG: u8 = 1;
const M_META_LEN: u8 = 2;
const M_DATA_LEN: u8 = 3;
const M_VALUE: u8 = 4;
const M_PROP: u8 = 5;


struct ZcMeta {
    types: Vec<u8>,
    values: Vec<u16>,
}

impl ZcMeta {
    fn clear(&mut self) {
        self.types.clear();
        self.values.clear();
    }

    fn push(&mut self, value_type: u8, value: u16) {
        self.values.push(value);
        self.types.push(value_type);
    }

    fn set(&mut self, value_type: u8, index: usize, value: u16) {
        if self.types[index] != value_type {
            panic!();
        }
        self.values[index] = value
    }
}

pub struct ZcProp {
    id: u16,
    name: Vu8,
}

pub struct ZcParser {
    input_index: usize,
    stack: Vec<ParseState>,
    meta: ZcMeta,
}

impl Default for ZcParser {
    fn default() -> ZcParser {
        ZcParser {
            input_index: 0,
            stack: vec!(),
            meta: ZcMeta { values: vec!(), types: vec!() },
        }
    }
}

impl ZcParser {
    fn clear(&mut self) {
        self.input_index = 0;
        self.stack.clear();
        self.meta.clear();
    }


    pub fn parse(&mut self, input: &Vu8) {
        self.clear();

        self.stack.push(Bottom);
        self.stack.push(Value);

        loop {
            let lookahead: u8 = input[self.input_index];
            let lookahead_type: u8 = to_lookahead_type(lookahead);
            let state: &ParseState = self.stack.last().unwrap();
            #[cfg(debug_assertions)]
            println!("{:?}", self.stack);

            match state {
                Bottom => { break; }
                // A mandatory str, number, array or map.
                Value => match lookahead_type {
                    BT_BLANK => {
                        self.next_char(input);
                    }
                    BT_DIGIT => {
                        self.stack.pop();
                        self.open_int(input);
                    }
                    BT_DOUBLE_QUOTE => {
                        self.stack.pop();
                        self.open_str(input);
                    }
                    BT_OPEN_ARR => {
                        self.stack.pop();
                        self.open_arr(input);
                    }
                    BT_OPEN_MAP => {
                        self.stack.pop();
                        self.open_map(input);
                    }
                    other => {
                        print_unexpected_char(&self.stack, lookahead, self.input_index);
                        break;
                    }
                },
                Digit { data_start } => match lookahead_type {
                    BT_DIGIT => {
                        self.next_char(input);
                    }
                    BT_BLANK | BT_COMMA => {
                        self.close_digit(input);
                    }
                    BT_CLOSE_ARR | BT_CLOSE_MAP | BT_TERMINAL => {
                        self.close_digit(input);
                    }
                    other => {
                        print_unexpected_char(&self.stack, lookahead, self.input_index);
                        break;
                    }
                },
                Str { data_start } => match lookahead_type {
                    BT_DOUBLE_QUOTE => {
                        self.close_str(input);
                    }
                    BT_TERMINAL => {
                        panic!()
                    }
                    other => {
                        self.next_char(input);
                    }
                },
                Arr { elem_count, meta_start, meta_len_index, data_len_index } => match lookahead_type {
                    BT_BLANK | BT_COMMA => {
                        self.next_char(input);
                    }
                    BT_DIGIT => {
                        self.open_int(input);
                    }
                    BT_DOUBLE_QUOTE => {
                        self.open_str(input);
                    }
                    BT_OPEN_ARR => {
                        self.open_arr(input);
                    }
                    BT_OPEN_MAP => {
                        self.open_map(input);
                    }

                    BT_CLOSE_ARR => {
                        self.close_arr(input);
                    }
                    _ => {
                        print_unexpected_char(&self.stack, lookahead, self.input_index);
                        break;
                    }
                },
                Map { elem_count, meta_start, meta_len_index, data_len_index } => match lookahead_type {
                    BT_BLANK | BT_COMMA => {
                        self.next_char(input);
                    }
                    BT_DOUBLE_QUOTE => {
                        self.open_map_key(input);
                    }
                    BT_CLOSE_MAP => {
                        self.close_map(input);
                    }
                    _ => {
                        print_unexpected_char(&self.stack, lookahead, self.input_index);
                        break;
                    }
                },
                MapKey { data_start } => match lookahead_type {
                    BT_DOUBLE_QUOTE => {
                        self.next_char(input);
                        self.stack.pop();
                    }
                    other => {
                        self.next_char(input);
                    }
                },
                MapColon => match lookahead_type {
                    BT_BLANK => {
                        self.next_char(input);
                    }
                    BT_COLON => {
                        self.next_char(input);
                        self.stack.pop();
                    }
                    other => {
                        self.next_char(input);
                    }
                },
                other => {
                    println!("Unsupported State: {:?}", other);
                    break;
                }
            }
        }
    }


    fn open_int(&mut self, input: &Vu8) {
        self.push(Digit { data_start: self.input_index });
        self.next_char(input);
    }


    #[inline(always)]
    fn open_map(&mut self, input: &Vu8) {
        let meta_start = self.meta.values.len();
        self.meta.push(M_TAG, TAG_MAP as u16);
        let meta_len_index = self.meta.values.len();
        self.meta.push(M_META_LEN, 0);
        let data_len_index = self.meta.values.len();
        self.meta.push(M_DATA_LEN, 0);

        let state = Map { elem_count: 0, meta_start, meta_len_index, data_len_index };
        self.push(state);

        self.next_char(input);
    }

    #[inline(always)]
    fn open_arr(&mut self, input: &Vu8) {
        let meta_start = self.meta.values.len();
        self.meta.push(M_TAG, TAG_MAP as u16);
        let meta_len_index = self.meta.values.len();
        self.meta.push(M_META_LEN, 0);
        let data_len_index = self.meta.values.len();
        self.meta.push(M_DATA_LEN, 0);

        let state = Arr { elem_count: 0, meta_start, meta_len_index, data_len_index };
        self.push(state);

        self.next_char(input);
    }

    #[inline(always)]
    fn close_map(&mut self, input: &Vu8) {
        let popped = self.stack.pop().unwrap();
        match popped {
            Map { elem_count, meta_start, meta_len_index, data_len_index } => {
                self.set_lenghts(elem_count, meta_start, meta_len_index, data_len_index);
            }
            other => {
                print_unexpected_state(&self.stack, &popped, "Map");
                panic!()
            }
        }

        self.next_char(input);
    }

    #[inline(always)]
    fn close_arr(&mut self, input: &Vu8) {
        let popped = self.stack.pop().unwrap();
        match popped {
            Arr { elem_count, meta_start, meta_len_index, data_len_index } => {
                self.set_lenghts(elem_count, meta_start, meta_len_index, data_len_index);
            }
            other => {
                print_unexpected_state(&self.stack, &popped, "Map");
                panic!()
            }
        }

        self.next_char(input);
    }


    #[inline(always)]
    fn close_digit(&mut self, input: &Vu8) {
        let popped = self.stack.pop().unwrap();
        match popped {
            Digit { data_start: start } => {
                self.meta.push(M_TAG, TAG_INT as u16);
                self.meta.push(M_META_LEN, 4);
                self.meta.push(M_DATA_LEN, (self.input_index - start) as u16);
                self.meta.push(M_VALUE, start as u16);
            }
            other => {
                print_unexpected_state(&self.stack, &popped, "Digit");
                panic!()
            }
        }
    }

    #[inline(always)]
    fn open_str(&mut self, input: &Vu8) {
        self.push(Str { data_start: self.input_index });
        self.next_char(input);
    }

    #[inline(always)]
    fn open_map_key(&mut self, input: &Vu8) {
        self.push(MapKey { data_start: self.input_index });
        self.next_char(input);
    }

    #[inline(always)]
    fn close_str(&mut self, input: &Vu8) {
        let popped = self.stack.pop().unwrap();
        match popped {
            Str { data_start: start } => {
                self.meta.push(M_TAG, TAG_STR as u16);
                self.meta.push(M_META_LEN, 4);
                self.meta.push(M_DATA_LEN, (self.input_index - start) as u16);
                self.meta.push(M_VALUE, start as u16);
            }
            other => {
                print_unexpected_state(&self.stack, &popped, "Str");
                panic!()
            }
        }
        self.next_char(input);
    }

    #[inline(always)]
    fn close_map_key(&mut self, input: &Vu8) {
        let popped = self.stack.pop().unwrap();
        match popped {
            MapKey { data_start: start } => {
                self.meta.push(M_TAG, TAG_STR as u16);
                self.meta.push(M_META_LEN, 4);
                self.meta.push(M_DATA_LEN, (self.input_index - start) as u16);
                self.meta.push(M_VALUE, start as u16);
            }
            other => {
                print_unexpected_state(&self.stack, &popped, "Str");
                panic!()
            }
        }
        self.next_char(input);
    }

    fn set_lenghts(&mut self, elem_count: u16, meta_start: usize, meta_len_index: usize, data_len_index: usize) {
        let data_len = elem_count;
        let meta_len = (self.meta.values.len() - meta_start) as u16;
        self.meta.set(M_META_LEN, meta_len_index, meta_len);
        self.meta.set(M_DATA_LEN, data_len_index, data_len);
    }

    #[inline(always)]
    fn push(&mut self, state: ParseState) {
        self.stack.push(state);
    }

    #[inline(always)]
    fn pop_push(&mut self, state: ParseState) {
        self.stack.pop();
        self.stack.push(state);
    }

    fn next_char(&mut self, input: &Vu8) {
        let u8 = input[self.input_index];
        self.input_index += 1;
        let char: char = char::from_u32(u8 as u32).unwrap();
        #[cfg(debug_assertions)]
        println!("  {:?}", char);
    }
}


fn print_unexpected_char(stack: &Vec<ParseState>, lookahead: u8, i: usize) {
    println!("Unexpected Character {:?} at {:?}", lookahead as char, i);
    panic!()
}

fn print_unexpected_state(stack: &Vec<ParseState>, actual: &ParseState, expected: &str) {
    println!("Unexpected state: stack={:?}, actual={:?}, expected={:?}", stack, actual, expected);
    panic!()
}

#[inline(always)]
pub fn to_lookahead_type(lookahead: u8) -> u8 {
    // Needs to be without conditional statements to avoid branch mis-predictions
    return is_digit(lookahead) * BT_DIGIT |
        is_alpha(lookahead) * BT_ALPHA |
        is_equal(lookahead, b'{') * BT_OPEN_MAP |
        is_equal(lookahead, b'[') * BT_OPEN_ARR |
        is_equal(lookahead, b'}') * BT_CLOSE_MAP |
        is_equal(lookahead, b']') * BT_CLOSE_ARR |
        is_equal(lookahead, b'"') * BT_DOUBLE_QUOTE |
        is_equal(lookahead, b' ') * BT_BLANK |
        is_equal(lookahead, b'\t') * BT_BLANK |
        is_equal(lookahead, b'\r') * BT_BLANK |
        is_equal(lookahead, b'\n') * BT_BLANK |
        is_equal(lookahead, b',') * BT_COMMA |
        is_equal(lookahead, b':') * BT_COLON |
        is_equal(lookahead, 0) * BT_TERMINAL;
}

pub fn is_alpha(lookahead: u8) -> u8 {
    return (b'a' <= lookahead && lookahead <= b'z' || b'A' <= lookahead && lookahead <= b'Z' || b'_' == lookahead) as u8;
}

pub fn is_digit(lookahead: u8) -> u8 {
    return (b'0' <= lookahead && lookahead <= b'9') as u8;
}

pub fn is_equal(lookahead: u8, target: u8) -> u8 {
    return (lookahead == target) as u8;
}

fn to_string(vec: &Vec<u8>) {
    String::from_utf8(vec.clone()).unwrap();
}

/*
fn test0() {
    let x: u64 = 0;
    let mut v: Vec<u64> = vec!();
    test1(&mut v, &mut v, x);
    test1(&mut v, &mut v, x);
}

fn test1(v1: &mut Vec<u64>, v2: &mut Vec<u64>, x: u64) {}

fn test2() {}

*/

#[cfg(test)]
mod tests {
    use crate::ZcParser;

    #[test]
    fn parse1() {
        let mut parser = ZcParser::default();

        parse(&mut parser, to_variants("3."));
        parse(&mut parser, to_variants(r###""asdf""###));
        parse(&mut parser, to_variants(r###"[]"###));
        parse(&mut parser, to_variants(r###"[1]"###));
        parse(&mut parser, vec!(r###"[123,456]"###.to_string()));
        parse(&mut parser, vec!(r###" [ 123 , 456 ] "###.to_string()));
        parse(&mut parser, to_variants(r###"[1, []]"###));
        parse(&mut parser, to_variants(r###"["asdf"]"###));
        parse(&mut parser, to_variants(r###"["asdf",1]"###));
        parse(&mut parser, to_variants(r###"["asdf",[1,[]]]"###));
        parse(&mut parser, to_variants(r###"{}"###));
        parse(&mut parser, to_variants(r###"{"kk":{}}"###));
        parse(&mut parser, to_variants(r###"[{},{}]"###));
        parse(&mut parser, to_variants(r###"{"aaa":222}"###));
        parse(&mut parser, to_variants(r###"{"id":1,"map":{"uno":1,"dos":2},"vec":[1,1]}"###));
    }


    fn parse(parser: &mut ZcParser, variants: Vec<String>) {
        for variant in variants.iter() {
            println!("---------");
            println!("{:?}", variant);
            let vec: Vec<u8> = append_zero(variant);
            parser.parse(&vec);
        }
    }


    fn append_zero(str: &String) -> Vec<u8> {
        let mut clone: Vec<u8> = str.clone().into_bytes();
        clone.push(0);
        return clone;
    }

    fn to_variants(str1: &str) -> Vec<String> {
        let mut char_vec: Vec<char> = vec!();
        char_vec.push(' ');
        for c in str1.chars() {
            if c == '"' || c == ':' || c == ',' || c == '{' || c == '}' || c == '[' || c == ']' || c == '1' {
                char_vec.push(' ');
            }
            char_vec.push(c);
        }
        char_vec.push(' ');

        let str2 = String::from_iter(char_vec);
        return vec!(str1.to_string(), str2);
    }
}