require 'nokogiri'
require 'pry'


MAX_NUMBER = 1e3

def get_commonness(priority)
  # explanations on commonness: https://jedict.com/HTML/edict_doc.html
  case priority
  when "news1", "gai1"; 23
  when "ichi1"; 20
  when "spec1"; 25
  when "news2", "gai2"; 50
  when "spec2"; 45
  when "ichi2"; 70
  when /nf\d+/;
    # can be an nfXX string. XX corresonds to the bracket of 500 words
    # the word belongs to. Example: nf23 => 23 * 500 = 11500 (between 11000 and 11500)
    # Incidentally, keeping this number works reasonably well with the above scoring.
    commonness_bracket = Integer(priority.match(/\d+/).to_s.gsub(/(^0*)/, ""))
    commonness_bracket
  else MAX_NUMBER
  end
end

def get_best_score(priority_list)
    if (priority_list.empty?)
        return MAX_NUMBER
    end
    return (priority_list.map { |unit| get_commonness(unit) }).min
end

def get_entry_score(entry)
  first_word = entry.xpath(".//keb").first
  priority_list = first_word.xpath("..//ke_pri").map {|unit| "#{unit.text}" }
  score = get_best_score(priority_list)
  puts "#{first_word.text}\t#{priority_list}\t#{score}"
  return score
end


doc = Nokogiri::XML(File.open("JMDictAssetsBuilder/JMDict.xml")) do |config|
  config.strict.noblanks
end

entries = doc.xpath("//entry")
nicechar = '丁'
selected = []
entries.each do |unit|
  k_ele = unit.css("k_ele")
  if !k_ele.empty?
    keb = k_ele.css("keb")
      if !keb.empty?
        if keb.first.text.include?(nicechar)
           selected.append(unit)
           # puts "Found #{keb.first.text}"
           get_entry_score(unit)
        end
      end
  end
end

puts selected.size

sorted = selected.sort_by { |unit|
  get_entry_score(unit)
}

# display first 10, or N

number_of_examples = [20, sorted.size].min - 1
puts  sorted.size, number_of_examples
for i in 0..number_of_examples do
    priority_list = sorted[i].xpath(".//keb").first.xpath("..//ke_pri").map {|unit| "#{unit.text}" }
    puts "#{i}\t#{sorted[i].xpath(".//keb").first.text}   ---   #{priority_list} --> #{get_best_score(priority_list)}"
end

# get char hex value
nicehex = nicechar.ord.to_s(16)

