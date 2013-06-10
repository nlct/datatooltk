\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{datatooltk}[2013/06/06]

\DeclareOption*{\PassOptionsToClass{\CurrentOption}{scrreprt}}

\ProcessOptions

\LoadClass{scrreprt}

\RequirePackage{etoolbox}

\renewcommand{\@author}{}

\newcommand{\Author}[1]{%
  \ifdefempty\@author
  {\def\@author{#1}}%
  {\appto\@author{\and #1}}%
}

\newcommand*{\sty}[1]{\textsf{#1}\index{#1 package@\textsf{#1}
package}}

\newcommand*{\opt}[1]{\texttt{-\/-#1}\index{--#1@\texttt{-\/-#1}}}

\newcommand*{\menu}[1]{\textsf{#1}}
\newcommand*{\mto}{$\to$}

\newcommand*{\lessthan}{\ifmmode<\else\textless\fi}
\newcommand*{\greaterthan}{\ifmmode>\else\textgreater\fi}
\newcommand*{\nbsp}{~}
\newcommand*{\doublequote}{"}
\newcommand*{\ampersand}{\&}
\newcommand*{\leftbrace}{\{}
\newcommand*{\rightbrace}{\}}
\newcommand*{\underscore}{\_}
\newcommand*{\textpercent}{\%}
\newcommand*{\texthash}{\#}
\newcommand{\floatconts}[3]{%
  \@ifundefined{\@captype conts}{\tableconts{#1}{#2}{#3}}%
  {\csname\@captype conts\endcsname{#1}{#2}{#3}}%
}

\newcommand{\tableconts}[3]{%
  #2\label{#1}\vskip\baselineskip
  {\centering #3\par}%
}

\newcommand{\figureconts}[3]{%
  {\centering #3\par}%
  \vskip\baselineskip
  #2\label{#1}%
}

\newcommand*{\sectionautorefname}{Section}

\newcommand*{\dq}[1]{``#1''}
\endinput
